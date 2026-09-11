import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CheckoutPage from '../pages/CheckoutPage';
import { AuthProvider } from '../context/AuthContext';
import { CartProvider } from '../context/CartContext';
import * as cartApi from '../api/cartApi';
import * as orderApi from '../api/orderApi';

vi.mock('../api/cartApi');
vi.mock('../api/orderApi');

const navigateMock = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => navigateMock };
});

function renderCheckoutPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <CartProvider>
          <CheckoutPage />
        </CartProvider>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('CheckoutPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
  });

  it('prompts to log in when not authenticated', async () => {
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });

    renderCheckoutPage();

    expect(await screen.findByText(/need to be logged in/i)).toBeInTheDocument();
  });

  it('shows an empty-cart message when authenticated but the cart is empty', async () => {
    localStorage.setItem('bookstore_refresh_token', 'existing-refresh-token');
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });

    renderCheckoutPage();

    expect(await screen.findByText(/cart is empty/i)).toBeInTheDocument();
  });

  it('shows the cart summary and defaults to cash on delivery', async () => {
    localStorage.setItem('bookstore_refresh_token', 'existing-refresh-token');
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 }],
      totalAmount: 71.98,
    });

    renderCheckoutPage();

    expect(await screen.findByText('Clean Code')).toBeInTheDocument();
    expect(screen.getByLabelText(/cash on delivery/i)).toBeChecked();
    expect(screen.queryByLabelText(/card number/i)).not.toBeInTheDocument();
  });

  it('selecting Credit card reveals the card fields', async () => {
    localStorage.setItem('bookstore_refresh_token', 'existing-refresh-token');
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 1, subtotal: 35.99 }],
      totalAmount: 35.99,
    });
    const user = userEvent.setup();

    renderCheckoutPage();
    await screen.findByText('Clean Code');
    await user.click(screen.getByLabelText(/credit card/i));

    expect(screen.getByLabelText(/card number/i)).toBeInTheDocument();
  });

  it('submitting cash on delivery calls checkout and navigates to the order', async () => {
    localStorage.setItem('bookstore_refresh_token', 'existing-refresh-token');
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 1, subtotal: 35.99 }],
      totalAmount: 35.99,
    });
    vi.mocked(orderApi.checkout).mockResolvedValue({
      id: 42, status: 'CREATED', totalAmount: 35.99, paymentType: 'CASH_ON_DELIVERY', items: [], createdAt: '',
    });
    const user = userEvent.setup();

    renderCheckoutPage();
    await screen.findByText('Clean Code');
    await user.click(screen.getByRole('button', { name: /place order/i }));

    expect(orderApi.checkout).toHaveBeenCalledWith({
      paymentType: 'CASH_ON_DELIVERY', cardNumber: undefined, cardExpiry: undefined,
    });
    expect(navigateMock).toHaveBeenCalledWith('/orders/42');
  });

  it('shows the server error message when checkout fails', async () => {
    localStorage.setItem('bookstore_refresh_token', 'existing-refresh-token');
    vi.mocked(cartApi.getCart).mockResolvedValue({
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 1, subtotal: 35.99 }],
      totalAmount: 35.99,
    });
    vi.mocked(orderApi.checkout).mockRejectedValue({
      response: { data: { message: 'Not enough stock for this book' } },
    });
    const user = userEvent.setup();

    renderCheckoutPage();
    await screen.findByText('Clean Code');
    await user.click(screen.getByRole('button', { name: /place order/i }));

    expect(await screen.findByText('Not enough stock for this book')).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
