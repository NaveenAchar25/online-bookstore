import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../App';
import * as bookApi from '../api/bookApi';
import * as cartApi from '../api/cartApi';

vi.mock('../api/bookApi');
vi.mock('../api/cartApi');

describe('App', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });
  });

  it('renders the book catalog at the root route', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue({
      items: [{ id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10 }],
      page: 0, size: 12, totalElements: 1, totalPages: 1,
    });

    render(<App />);

    expect(screen.getByRole('heading', { name: 'Books' })).toBeInTheDocument();
    expect(await screen.findByText('Clean Code')).toBeInTheDocument();
  });

  it('shows the nav bar with links to the cart and to log in', () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue({ items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 });

    render(<App />);

    expect(screen.getByRole('link', { name: 'Bookstore' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /cart/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /log in/i })).toBeInTheDocument();
  });
});
