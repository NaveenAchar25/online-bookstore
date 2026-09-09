import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookList from '../components/BookList';
import * as bookApi from '../api/bookApi';

vi.mock('../api/bookApi');

function renderBookList() {
  return render(
    <MemoryRouter>
      <BookList />
    </MemoryRouter>
  );
}

describe('BookList', () => {
  beforeEach(() => {
    vi.resetAllMocks();
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
});
