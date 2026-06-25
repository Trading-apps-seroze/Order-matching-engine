import { useCallback, useEffect, useState } from 'react'
import OrderForm from './components/OrderForm'
import OrderBookView from './components/OrderBookView'
import TradesView from './components/TradesView'
import { getOrderBook, getTrades } from './api'

const REFRESH_MS = 1000

export default function App() {
  const [book, setBook] = useState(null)
  const [trades, setTrades] = useState([])
  const [connected, setConnected] = useState(true)

  // Polls the read endpoints. (Milestone 6 replaces this with a WebSocket push.)
  const refresh = useCallback(async () => {
    try {
      const [b, t] = await Promise.all([getOrderBook(), getTrades()])
      setBook(b)
      setTrades(t)
      setConnected(true)
    } catch {
      setConnected(false)
    }
  }, [])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, REFRESH_MS)
    return () => clearInterval(id)
  }, [refresh])

  return (
    <div className="app">
      <header>
        <h1>Order Book Engine</h1>
        <span className={connected ? 'status ok' : 'status down'}>
          {connected ? 'connected' : 'backend unreachable'}
        </span>
      </header>

      <main>
        <section className="tickets">
          <OrderForm side="BUY" onSubmitted={refresh} />
          <OrderForm side="SELL" onSubmitted={refresh} />
        </section>
        <OrderBookView book={book} />
        <TradesView trades={trades} />
      </main>
    </div>
  )
}
