import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CartProvider, useCart } from '../context/CartContext';
import * as cartApi from '../api/cartApi';

vi.mock('../api/cartApi');

function TestConsumer() {
  const { cart, error, itemCount, addItem } = useCart();
  return (
    <div>
      <span data-testid="item-count">{itemCount}</span>
      {error && <span data-testid="error">{error}</span>}
      {cart && <span data-testid="total">{cart.totalAmount}</span>}
      <button onClick={() => addItem(1, 1).catch(() => {})}>Add</button>
    </div>
  );
}

describe('CartContext', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('loads the cart on mount and exposes its item count', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [
        { bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 },
        { bookId: 2, title: 'Effective Java', unitPrice: 42.5, quantity: 1, subtotal: 42.5 },
      ],
      totalAmount: 114.48,
    });

    render(
      <CartProvider>
        <TestConsumer />
      </CartProvider>
    );

    expect(await screen.findByTestId('total')).toHaveTextContent('114.48');
    expect(screen.getByTestId('item-count')).toHaveTextContent('3'); // 2 + 1
  });

  it('surfaces a server error message without crashing when add fails', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });
    vi.mocked(cartApi.addItem).mockRejectedValue({
      response: { data: { message: 'Not enough stock' } },
    });
    const user = userEvent.setup();

    render(
      <CartProvider>
        <TestConsumer />
      </CartProvider>
    );
    await screen.findByTestId('total');
    await user.click(screen.getByText('Add'));

    expect(await screen.findByTestId('error')).toHaveTextContent('Not enough stock');
  });
});
