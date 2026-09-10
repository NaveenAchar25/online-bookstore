import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NavBar from '../components/NavBar';
import { CartProvider } from '../context/CartContext';
import * as cartApi from '../api/cartApi';

vi.mock('../api/cartApi');

function renderNavBar() {
  return render(
    <MemoryRouter>
      <CartProvider>
        <NavBar />
      </CartProvider>
    </MemoryRouter>
  );
}

describe('NavBar', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('shows no count when the cart is empty', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });

    renderNavBar();

    expect(await screen.findByRole('link', { name: 'Cart' })).toBeInTheDocument();
  });

  it('shows the total item count once the cart loads', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [
        { bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 },
        { bookId: 2, title: 'Effective Java', unitPrice: 42.5, quantity: 1, subtotal: 42.5 },
      ],
      totalAmount: 114.48,
    });

    renderNavBar();

    expect(await screen.findByRole('link', { name: /cart \(3\)/i })).toBeInTheDocument();
  });
});
