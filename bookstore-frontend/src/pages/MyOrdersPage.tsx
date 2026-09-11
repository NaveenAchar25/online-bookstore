import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyOrders } from '../api/orderApi';
import type { Order } from '../types/Order';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

export default function MyOrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    getMyOrders()
      .then((data) => {
        if (!cancelled) setOrders(data);
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't load your orders. Please try again shortly.");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return <p className="state-message state-message--error">{error}</p>;
  }

  if (orders === null) {
    return <p className="state-message">Loading your orders…</p>;
  }

  if (orders.length === 0) {
    return (
      <div className="my-orders-page">
        <h1>Your orders</h1>
        <p className="state-message">
          You haven't placed any orders yet. <Link to="/">Browse books</Link>
        </p>
      </div>
    );
  }

  return (
    <div className="my-orders-page">
      <h1>Your orders</h1>
      <table className="cart-table">
        <thead>
          <tr>
            <th>Order</th>
            <th>Status</th>
            <th>Total</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id}>
              <td>
                <Link to={`/orders/${order.id}`}>#{order.id}</Link>
              </td>
              <td>{order.status}</td>
              <td>{currencyFormatter.format(order.totalAmount)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
