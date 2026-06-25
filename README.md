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

### Try it with curl

Start the server (`./gradlew bootRun`), then run these in order. They seed a
book, cross it, then exercise modify/cancel and the read endpoints.

```bash
BASE=http://localhost:8080
JSON='Content-Type: application/json'

# 1. Rest two sell orders and one buy (no crossing yet)
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"SELL","type":"LIMIT","price":101,"quantity":50}'   # -> orderId 1
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"SELL","type":"LIMIT","price":102,"quantity":30}'   # -> orderId 2
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"BUY","type":"LIMIT","price":100,"quantity":40}'    # -> orderId 3

# 2. Snapshot the book (best price first; ?depth=N to limit levels)
curl -s "$BASE/orderbook"
curl -s "$BASE/orderbook?depth=1"

# 3. Submit a crossing buy -> trades at the maker's price (101)
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"BUY","type":"LIMIT","price":101,"quantity":20}'    # -> orderId 4, FILLED

# 4. Market order (no price needed) sweeps the best asks
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"BUY","type":"MARKET","quantity":10}'

# 5. IOC / FOK
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"BUY","type":"IOC","price":102,"quantity":1000}'    # fills what it can, cancels rest
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"BUY","type":"FOK","price":102,"quantity":1000}'    # all-or-nothing -> CANCELLED

# 6. Modify the resting buy (orderId 3): note the id is in the URL, not the body.
#    Quantity-only decrease at the same price keeps queue position.
curl -s -X PUT $BASE/orders/3 -H "$JSON" -d '{"quantity":25,"price":100}'
#    Changing the price re-queues it (cancel + new under the same id).
curl -s -X PUT $BASE/orders/3 -H "$JSON" -d '{"quantity":25,"price":99}'

# 7. Cancel a resting order -> 204; cancelling an unknown/filled one -> 404
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $BASE/orders/3   # 204
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $BASE/orders/999 # 404

# 8. List every order seen, and the trade log
curl -s "$BASE/orders"
curl -s "$BASE/trades"

# 9. Validation error -> 400 with a JSON error body
curl -s -X POST $BASE/orders -H "$JSON" \
  -d '{"side":"BUY","type":"LIMIT","price":100,"quantity":0}'
```

> Tip: pipe any response through `| jq` for pretty-printed JSON.

Validation failures (non-positive quantity, modifying an order that isn't
resting) return `400` with a JSON `{"error": ...}` body.

## Building and testing

**Requires JDK 17 or newer** (built and tested with 21). Spring Boot 3.x needs
the JVM that *runs Gradle* to be 17+ — if you see
`Dependency requires at least JVM runtime version 17 ... This build uses a Java 8
JVM`, point Gradle at a 17+ JDK:

- **Terminal:** `export JAVA_HOME=/path/to/jdk21` before `./gradlew`.
- **IntelliJ:** Settings → Build Tools → Gradle → set **Gradle JVM** to 21.

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
