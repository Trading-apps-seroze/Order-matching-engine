# Order Book UI

A small React + Vite front end for the matching-engine REST API. It shows the
live order book and trade log and provides buy/sell tickets.

## Prerequisites

- Node.js 18+ and npm.
- The backend running on `http://localhost:8080` (`./gradlew bootRun` in
  `../backend`).

## Run

```bash
npm install      # first time only
npm run dev      # serves on http://localhost:5173
```

The Vite dev server proxies `/orders` (REST) and `/ws` (WebSocket) to the backend
on `:8080` (see `vite.config.js`), so the browser only talks to the Vite origin —
no CORS configuration needed.

## How it works

- `src/api.js` — thin fetch wrapper for submitting orders (REST).
- `src/useMarketData.js` — subscribes to the `/ws/marketdata` WebSocket; exposes
  the live book, the accumulated trade log, and a connection flag. Auto-reconnects
  and merges trades by id.
- `src/components/OrderForm.jsx` — buy/sell ticket (LIMIT/MARKET/IOC/FOK).
- `src/components/OrderBookView.jsx` — aggregated bids/asks.
- `src/components/TradesView.jsx` — recent trades.
- `src/App.jsx` — renders the feed; submitting an order triggers a server-side
  broadcast, so there's nothing to refresh by hand.

Updates are **pushed** over the WebSocket — no polling. The REST read endpoints
(`GET /orderbook`, `GET /trades`) still exist for ad-hoc queries.
