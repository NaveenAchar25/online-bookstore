import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { checkout } from '../api/orderApi';
import { extractErrorMessage } from '../lib/errorMessage';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

type PaymentType = 'CASH_ON_DELIVERY' | 'CREDIT_CARD';

export default function CheckoutPage() {
  const { isAuthenticated } = useAuth();
  const { cart, refresh: refreshCart } = useCart();
  const navigate = useNavigate();

  const [paymentType, setPaymentType] = useState<PaymentType>('CASH_ON_DELIVERY');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!isAuthenticated) {
    return (
      <div className="checkout-page">
        <h1>Checkout</h1>
        <p className="state-message">
          You need to be logged in to check out. <Link to="/login">Log in</Link> or{' '}
          <Link to="/register">create an account</Link> — your cart will still be here either way.
        </p>
      </div>
    );
  }

  if (cart === null) {
    return <p className="state-message">Loading your cart…</p>;
  }

  if (cart.items.length === 0) {
    return (
      <div className="checkout-page">
        <h1>Checkout</h1>
        <p className="state-message">
          Your cart is empty. <Link to="/">Browse books</Link>
        </p>
      </div>
    );
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const order = await checkout({
        paymentType,
        cardNumber: paymentType === 'CREDIT_CARD' ? cardNumber : undefined,
        cardExpiry: paymentType === 'CREDIT_CARD' ? cardExpiry : undefined,
      });
      // Checkout clears the cart server-side directly, bypassing every
      // mutation method CartContext normally tracks — without this, the
      // nav bar's item count would keep showing the pre-checkout total.
      await refreshCart();
      navigate(`/orders/${order.id}`);
    } catch (err) {
      setError(extractErrorMessage(err, 'Checkout failed. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="checkout-page">
      <h1>Checkout</h1>

      <table className="cart-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Quantity</th>
            <th>Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {cart.items.map((item) => (
            <tr key={item.bookId}>
              <td>{item.title}</td>
              <td>{item.quantity}</td>
              <td>{currencyFormatter.format(item.subtotal)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="cart-page__total">Total: {currencyFormatter.format(cart.totalAmount)}</p>

      <form onSubmit={handleSubmit} noValidate>
        <fieldset className="checkout-page__payment-choice">
          <legend>Payment method</legend>
          <label>
            <input
              type="radio"
              name="paymentType"
              value="CASH_ON_DELIVERY"
              checked={paymentType === 'CASH_ON_DELIVERY'}
              onChange={() => setPaymentType('CASH_ON_DELIVERY')}
            />
            Cash on delivery
          </label>
          <label>
            <input
              type="radio"
              name="paymentType"
              value="CREDIT_CARD"
              checked={paymentType === 'CREDIT_CARD'}
              onChange={() => setPaymentType('CREDIT_CARD')}
            />
            Credit card
          </label>
        </fieldset>

        {paymentType === 'CREDIT_CARD' && (
          <>
            <label htmlFor="checkout-card-number">Card number</label>
            <input
              id="checkout-card-number"
              value={cardNumber}
              onChange={(e) => setCardNumber(e.target.value)}
              placeholder="16 digits"
            />

            <label htmlFor="checkout-card-expiry">Expiry</label>
            <input
              id="checkout-card-expiry"
              value={cardExpiry}
              onChange={(e) => setCardExpiry(e.target.value)}
              placeholder="MM/YY"
            />
          </>
        )}

        {error && <p className="field-error">{error}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Placing order…' : 'Place order'}
        </button>
      </form>
    </div>
  );
}
