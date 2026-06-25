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
