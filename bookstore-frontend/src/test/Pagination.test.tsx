import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import Pagination from '../components/Pagination';

describe('Pagination', () => {
  it('renders nothing when there is only one page', () => {
    const { container } = render(<Pagination page={0} totalPages={1} onPageChange={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when there are zero pages', () => {
    const { container } = render(<Pagination page={0} totalPages={0} onPageChange={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('shows the current page as 1-indexed for humans, out of the total', () => {
    render(<Pagination page={2} totalPages={5} onPageChange={vi.fn()} />);

    expect(screen.getByText('Page 3 of 5')).toBeInTheDocument();
  });

  it('disables Previous on the first page', () => {
    render(<Pagination page={0} totalPages={3} onPageChange={vi.fn()} />);

    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /next/i })).toBeEnabled();
  });

  it('disables Next on the last page', () => {
    render(<Pagination page={2} totalPages={3} onPageChange={vi.fn()} />);

    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /previous/i })).toBeEnabled();
  });

  it('clicking Next calls onPageChange with page + 1', async () => {
    const onPageChange = vi.fn();
    const user = userEvent.setup();
    render(<Pagination page={1} totalPages={3} onPageChange={onPageChange} />);

    await user.click(screen.getByRole('button', { name: /next/i }));

    expect(onPageChange).toHaveBeenCalledWith(2);
  });

  it('clicking Previous calls onPageChange with page - 1', async () => {
    const onPageChange = vi.fn();
    const user = userEvent.setup();
    render(<Pagination page={1} totalPages={3} onPageChange={onPageChange} />);

    await user.click(screen.getByRole('button', { name: /previous/i }));

    expect(onPageChange).toHaveBeenCalledWith(0);
  });
});
