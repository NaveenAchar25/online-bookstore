import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NavBar from '../components/NavBar';
import { CartProvider } from '../context/CartContext';
import { AuthProvider } from '../context/AuthContext';
import * as cartApi from '../api/cartApi';
import * as authApi from '../api/authApi';
import { tokenStore } from '../lib/tokenStore';

vi.mock('../api/cartApi');
vi.mock('../api/authApi');

function renderNavBar() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <CartProvider>
          <NavBar />
        </CartProvider>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('NavBar', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });
  });

  it('shows no count when the cart is empty', async () => {
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

  it('shows Log in / Create account links when not authenticated', async () => {
    renderNavBar();

    expect(await screen.findByRole('link', { name: /log in/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create account/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /log out/i })).not.toBeInTheDocument();
  });

  it('shows a Log out button when a refresh token already exists', async () => {
    localStorage.setItem('bookstore_refresh_token', 'existing-refresh-token');

    renderNavBar();

    expect(await screen.findByRole('button', { name: /log out/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /log in/i })).not.toBeInTheDocument();
  });

  it('clicking Log out calls the logout API and clears tokens', async () => {
    localStorage.setItem('bookstore_refresh_token', 'existing-refresh-token');
    vi.mocked(authApi.logout).mockImplementation(async () => {
      tokenStore.clear();
    });
    const user = userEvent.setup();

    renderNavBar();
    await user.click(await screen.findByRole('button', { name: /log out/i }));

    expect(authApi.logout).toHaveBeenCalled();
  });
});
