// Thin wrapper over the matching-engine REST API. Paths are relative so the
// Vite dev proxy (see vite.config.js) forwards them to the backend on :8080.

async function asJson(res) {
  const text = await res.text()
  const body = text ? JSON.parse(text) : null
  if (!res.ok) {
    const message = body && body.error ? body.error : `${res.status} ${res.statusText}`
    throw new Error(message)
  }
  return body
}

export function submitOrder({ side, type, price, quantity }) {
  return fetch('/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ side, type, price, quantity }),
  }).then(asJson)
}

export function cancelOrder(id) {
  return fetch(`/orders/${id}`, { method: 'DELETE' }).then((res) => {
    if (!res.ok && res.status !== 404) throw new Error(`${res.status} ${res.statusText}`)
    return res.status !== 404
  })
}

export function getOrderBook(depth = 10) {
  return fetch(`/orderbook?depth=${depth}`).then(asJson)
}

export function getTrades() {
  return fetch('/trades').then(asJson)
}

export function getOrders() {
  return fetch('/orders').then(asJson)
}
