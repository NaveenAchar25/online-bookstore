import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../App';
import * as bookApi from '../api/bookApi';

vi.mock('../api/bookApi');

describe('App', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders the book catalog at the root route', async () => {
    vi.mocked(bookApi.getBooks).mockResolvedValue([
      { id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10 },
    ]);

    render(<App />);

    expect(screen.getByRole('heading', { name: 'Books' })).toBeInTheDocument();
    expect(await screen.findByText('Clean Code')).toBeInTheDocument();
  });
});
