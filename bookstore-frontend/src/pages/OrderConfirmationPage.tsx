import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { getOrder } from '../api/orderApi';
import type { Order } from '../types/Order';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

type LoadState =
  | { status: 'loading' }
  | { status: 'not-found' }
  | { status: 'error' }
  | { status: 'loaded'; order: Order };

const STATUS_LABELS: Record<Order['status'], string> = {
  CREATED: 'Placed — pay on delivery',
  PAID: 'Paid',
  CANCELLED: 'Cancelled',
};

export default function OrderConfirmationPage() {
  const { id } = useParams<{ id: string }>();
  const [state, setState] = useState<LoadState>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' });

    getOrder(Number(id))
      .then((order) => {
        if (!cancelled) setState({ status: 'loaded', order });
      })
      .catch((err) => {
        if (cancelled) return;
        if (isAxiosError(err) && err.response?.status === 404) {
          setState({ status: 'not-found' });
        } else {
          setState({ status: 'error' });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  if (state.status === 'loading') {
    return <p className="state-message">Loading your order…</p>;
  }

  if (state.status === 'not-found') {
    return (
      <div className="order-confirmation-page">
        <p className="state-message state-message--error">Couldn't find that order.</p>
        <Link to="/orders">Back to your orders</Link>
      </div>
    );
  }

  if (state.status === 'error') {
    return <p className="state-message state-message--error">Something went wrong loading this order.</p>;
  }

  const { order } = state;

  return (
    <div className="order-confirmation-page">
      <h1>Order #{order.id}</h1>
      <p className="order-confirmation-page__status">{STATUS_LABELS[order.status]}</p>

      <table className="cart-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Quantity</th>
            <th>Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {order.items.map((item) => (
            <tr key={item.bookId}>
              <td>{item.title}</td>
              <td>{item.quantity}</td>
              <td>{currencyFormatter.format(item.subtotal)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="cart-page__total">Total: {currencyFormatter.format(order.totalAmount)}</p>

      <Link to="/">Continue shopping</Link>
    </div>
  );
}
