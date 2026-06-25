import OrderForm from './components/OrderForm'
import OrderBookView from './components/OrderBookView'
import TradesView from './components/TradesView'
import useMarketData from './useMarketData'

export default function App() {
  // Live book + trades pushed over the WebSocket feed. Submitting an order via
  // REST triggers a server-side broadcast, so there's nothing to refresh by hand.
  const { book, trades, connected } = useMarketData()

  return (
    <div className="app">
      <header>
        <h1>Order Book Engine</h1>
        <span className={connected ? 'status ok' : 'status down'}>
          {connected ? 'live' : 'reconnecting…'}
        </span>
      </header>

      <main>
        <section className="tickets">
          <OrderForm side="BUY" />
          <OrderForm side="SELL" />
        </section>
        <OrderBookView book={book} />
        <TradesView trades={trades} />
      </main>
    </div>
  )
}
