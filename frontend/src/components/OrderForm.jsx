import { useState } from 'react'
import { submitOrder } from '../api'

// A buy or sell ticket. `side` is fixed per form ("BUY" / "SELL"); the user
// picks an order type and enters price/quantity.
export default function OrderForm({ side, onSubmitted }) {
  const [type, setType] = useState('LIMIT')
  const [price, setPrice] = useState('100')
  const [quantity, setQuantity] = useState('10')
  const [error, setError] = useState(null)

  const priceDisabled = type === 'MARKET' // market orders carry no price

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    try {
      await submitOrder({
        side,
        type,
        price: priceDisabled ? 0 : Number(price),
        quantity: Number(quantity),
      })
      onSubmitted?.()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <form className={`order-form ${side.toLowerCase()}`} onSubmit={handleSubmit}>
      <h2>{side}</h2>
      <label>
        Type
        <select value={type} onChange={(e) => setType(e.target.value)}>
          <option>LIMIT</option>
          <option>MARKET</option>
          <option>IOC</option>
          <option>FOK</option>
        </select>
      </label>
      <label>
        Price
        <input
          type="number"
          value={priceDisabled ? '' : price}
          disabled={priceDisabled}
          onChange={(e) => setPrice(e.target.value)}
        />
      </label>
      <label>
        Quantity
        <input
          type="number"
          min="1"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
      </label>
      <button type="submit" className={side.toLowerCase()}>
        {side === 'BUY' ? 'Buy' : 'Sell'}
      </button>
      {error && <p className="error">{error}</p>}
    </form>
  )
}
