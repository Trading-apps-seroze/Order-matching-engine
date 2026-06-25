# Roadmap

Status legend: ✅ done · 🚧 in progress · ⬜ not started

---

## Milestone 1 — Core Matching Engine (must have) ✅

The foundation.

**Domain**

- `Order`, `Trade`, `Side`, `OrderType`, `OrderStatus`, `PriceLevel`

**Order book structure**

```
TreeMap
  ↓
PriceLevel
  ↓
Queue<Order>
```

**Supported order types**

- ✅ LIMIT
- ✅ MARKET

**Rules**

- Price-time priority
- Partial fills
- Multiple price levels
- FIFO within a price level
- Trades execute at the **maker's** price

This is the heart of the project.

---

## Milestone 2 — Order Management ✅

Implemented as `OrderService` (in the `service` package), backed by an
`orderId → Order` index on `OrderBook` for O(1) lookup.

- ✅ `submit(side, type, price, qty)`
- ✅ `cancel(orderId)`
- ✅ `modify(orderId, newQty, newPrice)`

Resolved design questions (see [`LEARNINGS.md`](LEARNINGS.md) for the full
reasoning):

- **Quantity decrease** at the same price → edited **in place**, keeps time
  priority.
- **Quantity increase** or **price change** → handled as **cancel + new order**,
  loses queue position (a price change is a different queue; extra size can't cut
  ahead of those already waiting).

---

## Milestone 3 — Richer Order Types 🚧

After LIMIT and MARKET:

- ✅ IOC (immediate-or-cancel)
- ✅ FOK (fill-or-kill)

Later:

- ⬜ STOP
- ⬜ STOP_LIMIT

---

## Milestone 4 — REST API ✅

Spring Boot web layer in the `api` package; thin controllers over `OrderService`.

- ✅ `POST   /orders`        — submit
- ✅ `PUT    /orders/{id}`   — modify
- ✅ `DELETE /orders/{id}`   — cancel
- ✅ `GET    /orders`        — list
- ✅ `GET    /orderbook`     — aggregated snapshot (`?depth=N`)
- ✅ `GET    /trades`        — trade log

Controllers do almost nothing:

```
JSON
  ↓
DTO
  ↓
OrderService
  ↓
MatchingEngine
```

Run with `./gradlew bootRun` (port 8080). Validation errors map to `400` via a
`@RestControllerAdvice`.

---

## Milestone 5 — Live UI 🚧

Scaffolded as a React + Vite app in `frontend/` (buy/sell tickets, order book,
trade log). Live updates arrive over the WebSocket feed (Milestone 6) — no
polling. See [`frontend/README.md`](frontend/README.md).

What makes the project look polished — a simple React app.

```
+----------------------------+
| Buy Form                   |
+----------------------------+

+----------------------------+
| Sell Form                  |
+----------------------------+

+----------------------------+
| Trades                     |
+----------------------------+

+----------------------------+
| Order Book                 |
|                            |
| Bids       Asks            |
| 100 200    101 400         |
| 99  100    102 600         |
+----------------------------+
```

---

## Milestone 6 — WebSocket Updates ✅

Implemented. `OrderService` notifies a `MarketDataListener` after every mutation;
`MarketDataWebSocketHandler` (at `/ws/marketdata`) fans out `{ book, trades }`
frames to all connected browsers. The React UI subscribes via
`useMarketData.js` — no more polling.

Don't poll every second. Push instead:

```
New Trade
   │
   ▼
MatchingEngine
   │
   ▼
BookUpdatedEvent
   │
   ▼
WebSocket
   │
   ▼
Browser updates instantly
```

This feels much closer to a real trading application.

---

## Milestone 7 — Testing 🚧

Where many interview projects are weak.

**Unit tests**

- Simple match — `BUY 100` vs `SELL 100`
- Partial fill — `BUY 100` vs `SELL 30`
- FIFO — `BUY #1, #2, #3`; a `SELL` arrives → expect fills in order `#1, #2, #3`
- Multiple price levels — `SELL 100/101/102`, then `BUY @102`
- Market orders
- IOC
- FOK
- Cancel
- Modify

**Property tests**

Generate random orders, verify invariants:

- No negative quantity
- No crossed book after matching
- Total traded quantity is conserved
- FIFO preserved at each price level

Excellent interview talking points.

---

## Milestone 8 — Performance ⬜

Generate **1,000,000** orders. Measure:

- orders/sec
- average latency
- p99 latency

Later, experiment with:

- `ArrayDeque` vs custom linked list
- different data structures
- garbage generation

---

## Milestone 9 — Persistence ⬜

Right now, if the process dies, everything disappears. Instead:

```
Order
  ↓
Append to log
  ↓
Match
```

On startup:

```
Replay log
  ↓
Rebuild order book
```

Introduces event sourcing and recovery.

---

## Milestone 10 — Multiple Symbols ⬜

Instead of one order book:

```
Engine
  AAPL
  TSLA
  MSFT
  BTCUSD
  ETHUSD
```

Something like:

```
Map<String, OrderBook>
```

or

```
Map<String, MatchingEngine>
```

---

## Milestone 11 — Concurrency ⬜

Instead of:

```
REST Thread
  ↓
Matching Engine
```

Move to:

```
REST Threads
      │
      ▼
BlockingQueue<Order>
      │
      ▼
Single Matching Thread
```

Guarantees deterministic order processing and avoids locking inside the matching
engine.

---

## Milestone 12 — Observability ⬜

Add:

- execution latency
- orders received
- trades executed
- rejected orders
- current spread
- best bid / ask

Even simple logging and metrics make the project feel much more complete.
