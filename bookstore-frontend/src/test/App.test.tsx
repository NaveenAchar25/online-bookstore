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
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], totalAmount: 0 });
  });

  it('renders the book catalog at the root route', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue([
      { id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10 },
    ]);

    render(<App />);

    expect(screen.getByRole('heading', { name: 'Books' })).toBeInTheDocument();
    expect(await screen.findByText('Clean Code')).toBeInTheDocument();
  });

  it('shows the nav bar with a link to the cart', () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue([]);

    render(<App />);

    expect(screen.getByRole('link', { name: 'Bookstore' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /cart/i })).toBeInTheDocument();
  });
});
