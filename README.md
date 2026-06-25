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

## REST API

A Spring Boot web layer exposes the engine. Start it with:

```bash
./gradlew bootRun        # serves on http://localhost:8080
```

The controllers are deliberately thin — they only translate JSON DTOs to and
from `OrderService` calls (`JSON → DTO → OrderService → MatchingEngine`).

| Method   | Path             | Description                                    |
|----------|------------------|------------------------------------------------|
| `POST`   | `/orders`        | Submit a new order; returns the order + trades |
| `PUT`    | `/orders/{id}`   | Modify a resting order (quantity / price)      |
| `DELETE` | `/orders/{id}`   | Cancel a resting order (`204`, or `404`)       |
| `GET`    | `/orders`        | List every order the service has seen          |
| `GET`    | `/orderbook`     | Aggregated book snapshot (`?depth=N`, best first) |
| `GET`    | `/trades`        | The trade log, in execution order              |

Example:

```bash
# Submit a limit sell, then a crossing buy
curl -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"side":"SELL","type":"LIMIT","price":101,"quantity":50}'
curl -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"side":"BUY","type":"LIMIT","price":101,"quantity":20}'

curl localhost:8080/orderbook
curl localhost:8080/trades
```

Validation failures (bad quantity, unknown order on modify) return `400` with a
JSON `{"error": ...}` body.

## Building and testing

This project uses the Gradle wrapper, so no local Gradle install is needed.

```bash
# Run the test suite
./gradlew test

# Build (compile, test, package the runnable jar)
./gradlew build
```

## Project layout

```
src/main/java/com/orderbookengine/
  OrderBookEngineApplication.java   Spring Boot entry point
  api/        controllers + dto/    HTTP edge (JSON ↔ DTO)
  service/    OrderService, OrderResult
  engine/     MatchingEngine, OrderBook, PriceLevel
  model/      Order, Trade, Side, OrderType, OrderStatus
  config/     EngineConfig (bean wiring)
src/test/java/com/orderbookengine/
  MatchingEngineTest.java, OrderServiceTest.java
```

Dependency direction: `api → service → engine → model`.

## Roadmap

The current code covers the core matching engine and the four order types above.
Planned milestones — order management (cancel/modify), a REST API, a live UI,
WebSocket updates, persistence, multiple symbols, and more — are tracked in
[`ROADMAP.md`](ROADMAP.md).
