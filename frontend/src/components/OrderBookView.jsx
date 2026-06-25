// Renders the aggregated book: bids (descending) on the left, asks (ascending)
// on the right, best prices nearest the spread.
export default function OrderBookView({ book }) {
  const bids = book?.bids ?? []
  const asks = book?.asks ?? []
  const rows = Math.max(bids.length, asks.length)

  return (
    <div className="panel orderbook">
      <h2>Order Book</h2>
      <table>
        <thead>
          <tr>
            <th className="bid">Bid Qty</th>
            <th className="bid">Price</th>
            <th className="ask">Price</th>
            <th className="ask">Ask Qty</th>
          </tr>
        </thead>
        <tbody>
          {rows === 0 && (
            <tr>
              <td colSpan="4" className="empty">book is empty</td>
            </tr>
          )}
          {Array.from({ length: rows }).map((_, i) => (
            <tr key={i}>
              <td className="bid">{bids[i]?.quantity ?? ''}</td>
              <td className="bid price">{bids[i]?.price ?? ''}</td>
              <td className="ask price">{asks[i]?.price ?? ''}</td>
              <td className="ask">{asks[i]?.quantity ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
