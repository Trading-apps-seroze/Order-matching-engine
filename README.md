# Order Matching Engine

A limit order book and matching engine written in Java, modelling how an
exchange matches incoming orders against resting liquidity using **price-time
priority**.

## Features

- **Order types:** `LIMIT`, `MARKET`, `IOC` (immediate-or-cancel), and `FOK`
  (fill-or-kill).
- **Price-time priority:** best price first, then FIFO within each price level.
- **Partial fills** across multiple price levels.
- **Maker-price execution:** trades execute at the resting (maker) order's price.
- Clear order lifecycle: `NEW → PARTIALLY_FILLED → FILLED` / `CANCELLED`.

## Order types at a glance

| Type   | Rests on book? | Partial fill? | Leftover handling                        |
|--------|----------------|---------------|------------------------------------------|
| LIMIT  | Yes            | Yes           | Rests on the book                        |
| MARKET | No             | Yes           | Discarded                                |
| IOC    | No             | Yes           | Cancelled                                |
| FOK    | No             | No            | Whole order cancelled if it can't fill   |

See [`LEARNINGS.md`](LEARNINGS.md) for worked examples and the maker/taker
concept.

## Architecture

```
                 MatchingEngine
                       │
                       ▼
                   OrderBook
                       │
        ┌──────────────┴──────────────┐
        ▼                             ▼
  Bids: TreeMap                 Asks: TreeMap
  <price, PriceLevel>           <price, PriceLevel>
        │                             │
        ▼                             ▼
   PriceLevel                    PriceLevel
   (FIFO queue of Orders)        (FIFO queue of Orders)
```

- **`OrderBook`** — two `TreeMap`s (bids descending, asks ascending) so the best
  price is always first.
- **`PriceLevel`** — a FIFO queue of orders resting at one price.
- **`MatchingEngine`** — walks the opposite side of the book, generating
  `Trade`s, and rests or discards the taker's remainder according to its type.

## Building and testing

This project uses the Gradle wrapper, so no local Gradle install is needed.

```bash
# Run the test suite
./gradlew test

# Compile only
./gradlew build
```

## Project layout

```
src/main/java/com/orderbookengine/
  engine/   MatchingEngine, OrderBook, PriceLevel
  model/    Order, Trade, Side, OrderType, OrderStatus
src/test/java/com/orderbookengine/
  MatchingEngineTest.java
```

## Roadmap

The current code covers the core matching engine and the four order types above.
Planned milestones — order management (cancel/modify), a REST API, a live UI,
WebSocket updates, persistence, multiple symbols, and more — are tracked in
[`ROADMAP.md`](ROADMAP.md).
