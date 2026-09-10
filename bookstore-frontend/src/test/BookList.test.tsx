import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CartProvider } from '../context/CartContext';
import * as bookApi from '../api/bookApi';
import * as cartApi from '../api/cartApi';
import BookList from '../components/BookList';

vi.mock('../api/bookApi');
vi.mock('../api/cartApi');

function renderBookList() {
  return render(
    <MemoryRouter>
      <CartProvider>
        <BookList />
      </CartProvider>
    </MemoryRouter>
  );
}

describe('BookList', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });
  });

  it('shows a loading state, then renders fetched books as links to their detail page', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue([
      { id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10 },
      { id: 2, title: 'Effective Java', author: 'Joshua Bloch', price: 42.5, stockQuantity: 15 },
    ]);

    renderBookList();

    expect(screen.getByText(/loading/i)).toBeInTheDocument();

    const cleanCodeLink = await screen.findByRole('link', { name: /Clean Code/i });
    expect(cleanCodeLink).toHaveAttribute('href', '/books/1');
    expect(screen.getByText('Effective Java')).toBeInTheDocument();
    expect(screen.getByText('Robert C. Martin')).toBeInTheDocument();
    expect(screen.getByText('$35.99')).toBeInTheDocument();
  });

  it('shows an empty-state message when the catalog has no books', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue([]);

    renderBookList();

    expect(await screen.findByText(/no books/i)).toBeInTheDocument();
  });

  it('shows an error message if the catalog fails to load', async () => {
    vi.mocked(bookApi.getBooks).mockRejectedValue(new Error('Network error'));

    renderBookList();

    expect(await screen.findByText(/couldn't load the catalog/i)).toBeInTheDocument();
  });

  it('clicking "Add to cart" calls the cart API for that book', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue([
      { id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10 },
    ]);
    vi.mocked(cartApi.addItem).mockResolvedValue({ items: [], totalAmount: 0 });
    const user = userEvent.setup();

    renderBookList();
    await screen.findByText('Clean Code');
    await user.click(screen.getByRole('button', { name: /add to cart/i }));

    expect(cartApi.addItem).toHaveBeenCalledWith(1, 1);
  });
});
