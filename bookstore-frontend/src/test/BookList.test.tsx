import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookList from '../components/BookList';
import { CartProvider } from '../context/CartContext';
import * as bookApi from '../api/bookApi';
import * as cartApi from '../api/cartApi';

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

const onePage = (items: ReturnType<typeof makeBook>[]) => ({
  items, page: 0, size: 12, totalElements: items.length, totalPages: 1,
});

function makeBook(id: number, title: string, author: string, price: number) {
  return { id, title, author, price, stockQuantity: 10 };
}

describe('BookList', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });
  });

  it('shows a loading state, then renders fetched books as links to their detail page', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue(onePage([
      makeBook(1, 'Clean Code', 'Robert C. Martin', 35.99),
      makeBook(2, 'Effective Java', 'Joshua Bloch', 42.5),
    ]));

    renderBookList();

    expect(screen.getByText(/loading/i)).toBeInTheDocument();

    const cleanCodeLink = await screen.findByRole('link', { name: /Clean Code/i });
    expect(cleanCodeLink).toHaveAttribute('href', '/books/1');
    expect(screen.getByText('Effective Java')).toBeInTheDocument();
    expect(screen.getByText('$35.99')).toBeInTheDocument();
  });

  it('shows an empty-state message when the catalog has no books', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue(onePage([]));

    renderBookList();

    expect(await screen.findByText(/no books in the catalog/i)).toBeInTheDocument();
  });

  it('shows an error message if the catalog fails to load', async () => {
    vi.mocked(bookApi.getBooks).mockRejectedValue(new Error('Network error'));

    renderBookList();

    expect(await screen.findByText(/couldn't load the catalog/i)).toBeInTheDocument();
  });

  it('clicking "Add to cart" calls the cart API for that book', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue(onePage([
      makeBook(1, 'Clean Code', 'Robert C. Martin', 35.99),
    ]));
    vi.mocked(cartApi.addItem).mockResolvedValue({ items: [], totalAmount: 0 });
    const user = userEvent.setup();

    renderBookList();
    await screen.findByText('Clean Code');
    await user.click(screen.getByRole('button', { name: /add to cart/i }));

    expect(cartApi.addItem).toHaveBeenCalledWith(1, 1);
  });

  it('does not show pagination controls when there is only one page', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue(onePage([makeBook(1, 'Clean Code', 'Robert C. Martin', 35.99)]));

    renderBookList();

    await screen.findByText('Clean Code');
    expect(screen.queryByText(/page \d+ of \d+/i)).not.toBeInTheDocument();
  });

  it('clicking Next requests the next page', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue({
      items: [makeBook(1, 'Clean Code', 'Robert C. Martin', 35.99)],
      page: 0, size: 1, totalElements: 2, totalPages: 2,
    });
    const user = userEvent.setup();

    renderBookList();
    await screen.findByText('Clean Code');
    await user.click(screen.getByRole('button', { name: /next/i }));

    expect(bookApi.getBooks).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));
  });

  describe('debounced search', () => {
    it('does not call getBooks again until typing has paused', async () => {
      vi.mocked(bookApi.getBooks).mockResolvedValue(onePage([makeBook(1, 'Clean Code', 'Robert C. Martin', 35.99)]));
      const user = userEvent.setup();

      renderBookList();
      await screen.findByText('Clean Code'); // initial load settled — exactly 1 call so far

      const searchInput = screen.getByLabelText(/search books/i);
      await user.type(searchInput, 'python');

      // Immediately after typing, the debounce window (350ms) hasn't
      // elapsed yet — still just the one initial call.
      expect(bookApi.getBooks).toHaveBeenCalledTimes(1);

      // Real timers deliberately, not fake ones — combining fake timers
      // with userEvent's own internal async scheduling and this
      // component's promise-based fetch effect proved unreliable (timed
      // out waiting for microtasks that fake timers don't flush the same
      // way). A real ~400ms wait for a 350ms debounce is a small, honest
      // cost for a test that actually passes reliably.
      await vi.waitFor(
        () => expect(bookApi.getBooks).toHaveBeenLastCalledWith(expect.objectContaining({ query: 'python' })),
        { timeout: 1000 }
      );
    });
  });
});
