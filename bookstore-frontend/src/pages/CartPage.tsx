import { useEffect, useState } from 'react';
import { useCart } from '../context/CartContext';
import type { CartItem } from '../types/Cart';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

function QuantityCell({
  item,
  onCommit,
}: {
  item: CartItem;
  onCommit: (bookId: number, quantity: number) => void;
}) {
  const [value, setValue] = useState(String(item.quantity));

 
  useEffect(() => {
    setValue(String(item.quantity));
  }, [item.quantity]);

  function commit() {
    const parsed = Number(value);
    if (Number.isInteger(parsed) && parsed >= 1) {
      onCommit(item.bookId, parsed);
    } else {
      setValue(String(item.quantity)); // invalid entry — revert to the last confirmed value
    }
  }

  return (
    <input
      type="number"
      min={1}
      value={value}
      className="cart-table__quantity-input"
      onChange={(e) => setValue(e.target.value)}
      onBlur={commit}
      aria-label={`Quantity for ${item.title}`}
    />
  );
}

export default function CartPage() {
  const { cart, error, updateItemQuantity, removeItem } = useCart();

  if (error) {
    return <p className="state-message state-message--error">{error}</p>;
  }

  if (cart === null) {
    return <p className="state-message">Loading your cart…</p>;
  }

  if (cart.items.length === 0) {
    return (
      <div className="cart-page">
        <h1>Your cart</h1>
        <p className="state-message">Your cart is empty.</p>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <h1>Your cart</h1>
      <table className="cart-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Price</th>
            <th>Quantity</th>
            <th>Subtotal</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {cart.items.map((item) => (
            <tr key={item.bookId}>
              <td>{item.title}</td>
              <td>{currencyFormatter.format(item.unitPrice)}</td>
              <td>
                <QuantityCell item={item} onCommit={updateItemQuantity} />
              </td>
              <td>{currencyFormatter.format(item.subtotal)}</td>
              <td>
                <button type="button" onClick={() => removeItem(item.bookId)}>
                  Remove
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="cart-page__total">Total: {currencyFormatter.format(cart.totalAmount)}</p>
    </div>
  );
}
