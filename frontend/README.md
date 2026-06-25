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

The Vite dev server proxies `/orders`, `/orderbook`, and `/trades` to the backend
on `:8080` (see `vite.config.js`), so the browser only talks to the Vite origin —
no CORS configuration needed.

## How it works

- `src/api.js` — thin fetch wrapper over the REST endpoints.
- `src/components/OrderForm.jsx` — buy/sell ticket (LIMIT/MARKET/IOC/FOK).
- `src/components/OrderBookView.jsx` — aggregated bids/asks.
- `src/components/TradesView.jsx` — recent trades.
- `src/App.jsx` — polls the read endpoints once a second and refreshes
  immediately after you submit an order.

Polling is a placeholder; Milestone 6 (WebSocket push) will replace it with live
updates. See the root `ROADMAP.md`.
