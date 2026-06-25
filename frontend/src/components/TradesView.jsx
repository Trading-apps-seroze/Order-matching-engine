// Most-recent-first trade log.
export default function TradesView({ trades }) {
  const recent = [...(trades ?? [])].reverse()

  return (
    <div className="panel trades">
      <h2>Trades</h2>
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Price</th>
            <th>Qty</th>
            <th>Buy</th>
            <th>Sell</th>
          </tr>
        </thead>
        <tbody>
          {recent.length === 0 && (
            <tr>
              <td colSpan="5" className="empty">no trades yet</td>
            </tr>
          )}
          {recent.map((t) => (
            <tr key={t.tradeId}>
              <td>{t.tradeId}</td>
              <td className="price">{t.price}</td>
              <td>{t.quantity}</td>
              <td>{t.buyOrderId}</td>
              <td>{t.sellOrderId}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
