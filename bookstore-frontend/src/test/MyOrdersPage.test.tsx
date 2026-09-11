import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MyOrdersPage from '../pages/MyOrdersPage';
import * as orderApi from '../api/orderApi';

vi.mock('../api/orderApi');

function renderMyOrdersPage() {
  return render(
    <MemoryRouter>
      <MyOrdersPage />
    </MemoryRouter>
  );
}

describe('MyOrdersPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('shows an empty-state message when there are no orders', async () => {
    vi.mocked(orderApi.getMyOrders).mockResolvedValue([]);

    renderMyOrdersPage();

    expect(await screen.findByText(/haven't placed any orders/i)).toBeInTheDocument();
  });

  it('lists orders with a link to each one', async () => {
    vi.mocked(orderApi.getMyOrders).mockResolvedValue([
      {
        id: 42, status: 'PAID', totalAmount: 35.99, paymentType: 'CREDIT_CARD',
        items: [], createdAt: '2026-01-01T10:00:00',
      },
    ]);

    renderMyOrdersPage();

    const link = await screen.findByRole('link', { name: '#42' });
    expect(link).toHaveAttribute('href', '/orders/42');
    expect(screen.getByText('PAID')).toBeInTheDocument();
  });

  it('shows an error message if orders fail to load', async () => {
    vi.mocked(orderApi.getMyOrders).mockRejectedValue(new Error('Network error'));

    renderMyOrdersPage();

    expect(await screen.findByText(/couldn't load your orders/i)).toBeInTheDocument();
  });
});
