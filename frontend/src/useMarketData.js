import { useEffect, useRef, useState } from 'react'

// Subscribes to the market-data WebSocket and exposes the live book, the
// accumulated trade log, and a connection flag. The server pushes
// { book, trades } frames — a full snapshot on connect, then incremental frames
// (only the new trades) after each mutation. We replace the book each time and
// merge trades by id so reconnders/snapshots can't create duplicates.
export default function useMarketData() {
  const [book, setBook] = useState(null)
  const [trades, setTrades] = useState([])
  const [connected, setConnected] = useState(false)
  const tradesById = useRef(new Map())

  useEffect(() => {
    let ws
    let reconnectTimer
    let closed = false

    function connect() {
      const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
      ws = new WebSocket(`${proto}://${window.location.host}/ws/marketdata`)

      ws.onopen = () => setConnected(true)

      ws.onmessage = (event) => {
        const frame = JSON.parse(event.data)
        if (frame.book) setBook(frame.book)
        if (frame.trades?.length) {
          for (const t of frame.trades) tradesById.current.set(t.tradeId, t)
          setTrades([...tradesById.current.values()].sort((a, b) => a.tradeId - b.tradeId))
        }
      }

      ws.onclose = () => {
        setConnected(false)
        if (!closed) reconnectTimer = setTimeout(connect, 1000) // retry
      }

      ws.onerror = () => ws.close()
    }

    connect()
    return () => {
      closed = true
      clearTimeout(reconnectTimer)
      ws?.close()
    }
  }, [])

  return { book, trades, connected }
}
