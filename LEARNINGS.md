# Learnings

## Order Types

This engine supports two order types (see `OrderType.java`):

- **LIMIT** — Trades only at the order's specified price or better. If a limit
  order cannot be fully filled immediately, the remaining quantity **rests on
  the book** as a resting order, waiting for future matches.

- **MARKET** — Trades against the best available prices regardless of price,
  prioritizing immediate execution. **Market orders never rest on the book.**
  Any unfilled quantity is simply not added to the book (it is left unfilled or
  cancelled), because a market order has no price at which it could sit and wait.

## Time-in-Force: IOC and FOK

> **Note:** Both are implemented in this engine as `OrderType.IOC` and
> `OrderType.FOK`. They are limit-priced (so they honour the same crossing logic
> as `LIMIT`) but, like `MARKET`, they never rest — the difference is only in how
> the unfilled remainder is handled.

Both share one rule with MARKET: **they never rest on the book.** The difference
is *how much* of the order is allowed to execute before the remainder is killed.

- **IOC (Immediate-Or-Cancel)** — fill as much as possible *right now*, then
  cancel whatever is left. Partial fills are allowed.
- **FOK (Fill-Or-Kill)** — fill the order *completely and immediately*, or do
  nothing at all. Partial fills are **not** allowed; it's all-or-nothing.

### Shared starting book

```
SELL
100 -> 30 shares
101 -> 30 shares
```

### Example — IOC

Incoming: `BUY 50 @100 IOC`

- 30 shares trade at 100 (the level crosses).
- The level at 101 does **not** cross (limit price is 100).
- 20 shares remain → **cancelled**, not rested.

Result: 30 filled, 20 cancelled, status `PARTIALLY_FILLED`, nothing on the book.

### Example — FOK

Incoming: `BUY 50 @100 FOK`

- Only 30 shares are available at a crossing price (≤ 100).
- The full 50 **cannot** be filled immediately → the **entire order is killed**.

Result: 0 filled, status `CANCELLED`, the book is untouched.

Now compare with a fillable FOK — Incoming: `BUY 30 @100 FOK`

- 30 shares available at 100, exactly matching the order.
- Fully fills immediately → 30 filled, status `FILLED`.

### Quick comparison

| Type   | Rests on book? | Partial fill allowed? | Leftover after match |
|--------|----------------|-----------------------|----------------------|
| LIMIT  | Yes            | Yes                   | Rests on the book    |
| MARKET | No             | Yes                   | Discarded            |
| IOC    | No             | Yes                   | Cancelled            |
| FOK    | No             | No (all-or-nothing)   | Whole order cancelled if it can't fully fill |

## Stop and Stop-Limit Orders

> **Note:** *Not yet implemented* — planned next (`OrderType.STOP` and
> `OrderType.STOP_LIMIT`). Definitions captured here ahead of the work.

Everything so far (LIMIT, MARKET, IOC, FOK) becomes *active* the moment it
arrives. A **stop order is dormant**: it sits off to the side, *not on the book*,
until the market reaches a **trigger price**. Only then does it "wake up" and turn
into a normal order. Think of it as a conditional: *"once the price hits X, send
this order."*

A stop carries two prices:

- **Stop (trigger) price** — the level that arms the order.
- For a stop-*limit*, also a **limit price** — the price the order uses once
  triggered.

### STOP (a.k.a. stop-market)

When triggered, it fires a **MARKET** order — guaranteed to execute (if there's
liquidity), but at whatever price the book offers.

- **Sell stop** triggers when the market trades **at or below** the stop price.
  Classic stop-loss: "if it drops to 95, get me out at market."
- **Buy stop** triggers when the market trades **at or above** the stop price.
  Used to cap a short, or to buy into a breakout.

### STOP_LIMIT

When triggered, it places a **LIMIT** order at the pre-set limit price instead of
a market order. You control the worst price you'll accept, but you risk **not
filling at all** if the market gaps past your limit.

- Example sell stop-limit: stop `95`, limit `94`. If the price touches 95, a
  `SELL @94` limit order is placed. If the market crashes straight through 94,
  nothing fills and you're still holding.

### STOP vs STOP_LIMIT — the trade-off

| | Triggers into | Fill guaranteed? | Price guaranteed? |
|---|---|---|---|
| STOP        | MARKET order | Yes (if liquidity) | No  |
| STOP_LIMIT  | LIMIT order  | No                 | Yes (limit or better) |

### Why the trigger direction matters

The condition is always "the market moved *against* a level you care about":

- A **sell** stop protects a long position, so it triggers on the way **down**
  (price ≤ stop).
- A **buy** stop protects a short / chases a breakout, so it triggers on the way
  **up** (price ≥ stop).

Note this is the *opposite* of a limit order's crossing logic — a stop's trigger
is about the **last traded price** (or best bid/ask) reaching the level, not
about whether it crosses the spread.

### Implementation notes (for next time)

- Stops are **not** kept in the bid/ask `TreeMap`s — they're a separate pending
  set, keyed by trigger price, because they must not be matchable while dormant.
- Something has to **watch the market** and check pending stops after every
  trade (the last trade price is the natural trigger signal). On trigger, remove
  the stop from the pending set and route it through `MatchingEngine.process` as
  a MARKET (STOP) or LIMIT (STOP_LIMIT) order.
- Watch for **cascades**: one triggered stop can move the price and trip further
  stops in the same pass — process them in a loop until no more trigger.

## Modifying orders and time priority

When an order is already resting on the book, can you change it without losing
your place in the queue? The answer depends on whether the change makes the order
**more aggressive** (cutting ahead of others) or **less aggressive** (stepping
back). The guiding principle: *you may keep your queue position only if the
change cannot disadvantage anyone who was already waiting behind you.*

| Change                | Time priority | Why                                                                                                  |
|-----------------------|---------------|------------------------------------------------------------------------------------------------------|
| **Cancel**            | n/a           | The order leaves the book entirely — there is nothing left to prioritise.                             |
| **Quantity decrease** | **Kept** (edit in place) | You are *removing* liquidity. Nobody behind you is worse off — if anything they move up. So penalising you would be unfair, and exchanges let you shrink in place. |
| **Quantity increase** | **Lost** (cancel + new) | Extra size at the front of the queue jumps ahead of everyone who was already waiting at that price. That *is* a disadvantage to them, so the added size must go to the back. |
| **Price change**      | **Lost** (cancel + new) | A different price is a different queue. Your old position has no meaning at the new price, so you join the new price level at the back. |

So "modify = cancel + new" (your first instinct) is correct for **price changes
and size increases**, but a **size decrease is done in place** and keeps
priority — that is the one case worth special-casing.

### How this maps to the code

`OrderService.modify(orderId, newQuantity, newPrice)` implements exactly this:

- **Same price *and* quantity reduced** → `existing.setRemainingQty(newQuantity)`
  in place. The order keeps its spot at the head of its `PriceLevel` FIFO queue.
- **Otherwise** (price changed, or quantity increased) → `cancel(orderId)` then
  process a fresh order. It reuses the same order id (so callers keep addressing
  it) but gets a **new timestamp**, which is what sends it to the back of the
  queue.

This is also why `OrderService` — not the caller — owns timestamp generation: it
is the single authority on time, so "a newer timestamp loses priority" is always
true. A cancel routes straight to `OrderBook.removeOrder` (there is nothing to
match), while submit and the cancel+new branch of modify both go through
`MatchingEngine.process`, keeping all *matching* in one place.

## Maker vs Taker

Why is the incoming order called the **taker** and the matched (resting) order
the **maker**? It has nothing to do with BUY vs SELL — it has everything to do
with **where liquidity comes from**.

- **Maker** — an order that was *already resting on the order book*. It
  *provides* liquidity for others to trade against.
- **Taker** — a *newly arrived* order that *consumes existing liquidity* by
  matching against resting orders.

### Example 1 — buyer is the taker

Book:

```
SELL
100 -> 50 shares
101 -> 80 shares
```

Incoming: `BUY 30 @100`

The buyer took shares off the book, so the buyer **consumed liquidity**.

- Buyer = Taker
- Seller = Maker

### Example 2 — seller is the taker

Now reverse it. Book:

```
BUY
100 -> 50
99  -> 30
```

Incoming: `SELL 40 @100`

The seller removed liquidity. The buyers were already waiting (resting).

- Seller = Taker
- Buyers = Makers

So "maker" simply means *"this order was already resting on the book"* and
"taker" means *"this newly arrived order is consuming existing liquidity."* It
has absolutely nothing to do with buy/sell.

### Why this distinction matters

Exchanges often charge different fees to reward liquidity providers:

| Role  | Example fee |
|-------|-------------|
| Maker | 0.00%       |
| Taker | 0.05%       |

This is why, in the engine, the incoming order is the taker and the matched
resting order is the maker.
