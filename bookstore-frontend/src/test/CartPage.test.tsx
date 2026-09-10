import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CartPage from '../pages/CartPage';
import { CartProvider } from '../context/CartContext';
import * as cartApi from '../api/cartApi';

vi.mock('../api/cartApi');

function renderCartPage() {
  return render(
    <CartProvider>
      <CartPage />
    </CartProvider>
  );
}

describe('CartPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('shows an empty-cart message when there are no items', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });

    renderCartPage();

    expect(await screen.findByText(/your cart is empty/i)).toBeInTheDocument();
  });

  it('renders items, quantities, and the total', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 }],
      totalAmount: 71.98,
    });

    renderCartPage();

    expect(await screen.findByText('Clean Code')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2')).toBeInTheDocument();
    expect(screen.getByText(/total: \$71\.98/i)).toBeInTheDocument();
  });

  it('committing an edited quantity (on blur) calls updateItemQuantity', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 }],
      totalAmount: 71.98,
    });
    vi.mocked(cartApi.updateItemQuantity).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 5, subtotal: 179.95 }],
      totalAmount: 179.95,
    });
    const user = userEvent.setup();

    renderCartPage();
    const quantityInput = await screen.findByLabelText(/quantity for clean code/i);
    await user.clear(quantityInput);
    await user.type(quantityInput, '5');
    await user.tab(); // moves focus away, triggering onBlur — the actual commit point

    expect(cartApi.updateItemQuantity).toHaveBeenCalledWith(1, 5);
  });

  it('does not call updateItemQuantity while still typing, before blur', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 }],
      totalAmount: 71.98,
    });
    const user = userEvent.setup();

    renderCartPage();
    const quantityInput = await screen.findByLabelText(/quantity for clean code/i);
    await user.clear(quantityInput);
    await user.type(quantityInput, '5');

    expect(cartApi.updateItemQuantity).not.toHaveBeenCalled();
  });

  it('clicking Remove calls removeItem for that book', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 }],
      totalAmount: 71.98,
    });
    vi.mocked(cartApi.removeItem).mockResolvedValue({ items: [], totalAmount: 0 });
    const user = userEvent.setup();

    renderCartPage();
    await screen.findByText('Clean Code');
    await user.click(screen.getByRole('button', { name: /remove/i }));

    expect(cartApi.removeItem).toHaveBeenCalledWith(1);
  });
});
