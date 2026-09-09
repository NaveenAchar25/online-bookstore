import { render, screen } from '@testing-library/react';
import { AxiosError } from 'axios';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookDetailPage from '../pages/BookDetailPage';
import * as bookApi from '../api/bookApi';

vi.mock('../api/bookApi');

function renderAtBookDetail(id: string) {
  return render(
    <MemoryRouter initialEntries={[`/books/${id}`]}>
      <Routes>
        <Route path="/books/:id" element={<BookDetailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('BookDetailPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('shows a loading state, then renders the fetched book', async () => {
    vi.mocked(bookApi.getBookById).mockResolvedValue({
      id: 1,
      title: 'Clean Code',
      author: 'Robert C. Martin',
      price: 35.99,
      stockQuantity: 10,
      description: 'A handbook of agile software craftsmanship',
    });

    renderAtBookDetail('1');

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Clean Code' })).toBeInTheDocument();
    expect(screen.getByText('Robert C. Martin')).toBeInTheDocument();
    expect(screen.getByText('$35.99')).toBeInTheDocument();
    expect(screen.getByText('A handbook of agile software craftsmanship')).toBeInTheDocument();
    expect(bookApi.getBookById).toHaveBeenCalledWith(1);
  });

  it('shows a not-found message when the book does not exist', async () => {
    // A plain object shaped like { response: { status: 404 } } is NOT
    // enough here — isAxiosError() checks for axios's own internal marker,
    // which only a real AxiosError instance carries. A bare object silently
    // falls through to the generic error branch instead, which is exactly
    // the bug this test caught on its first run.
    const notFoundError = new AxiosError(
      'Request failed with status code 404',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      {
        status: 404,
        statusText: 'Not Found',
        headers: {},
        config: {} as never,
        data: { message: 'Book not found with id: 999' },
      }
    );
    vi.mocked(bookApi.getBookById).mockRejectedValue(notFoundError);

    renderAtBookDetail('999');

    expect(await screen.findByText(/couldn't find that book/i)).toBeInTheDocument();
  });

  it('shows a generic error message on any other failure', async () => {
    vi.mocked(bookApi.getBookById).mockRejectedValue(new Error('Network error'));

    renderAtBookDetail('1');

    expect(await screen.findByText(/something went wrong/i)).toBeInTheDocument();
  });
});
