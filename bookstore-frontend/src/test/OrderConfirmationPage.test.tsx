import { render, screen } from '@testing-library/react';
import { AxiosError } from 'axios';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrderConfirmationPage from '../pages/OrderConfirmationPage';
import * as orderApi from '../api/orderApi';

vi.mock('../api/orderApi');

function renderAtOrder(id: string) {
  return render(
    <MemoryRouter initialEntries={[`/orders/${id}`]}>
      <Routes>
        <Route path="/orders/:id" element={<OrderConfirmationPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('OrderConfirmationPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('shows the order status and line items once loaded', async () => {
    vi.mocked(orderApi.getOrder).mockResolvedValue({
      id: 42, status: 'PAID', totalAmount: 35.99, paymentType: 'CREDIT_CARD',
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 1, subtotal: 35.99 }],
      createdAt: '2026-01-01T10:00:00',
    });

    renderAtOrder('42');

    expect(await screen.findByRole('heading', { name: /order #42/i })).toBeInTheDocument();
    expect(screen.getByText('Paid')).toBeInTheDocument();
    expect(screen.getByText('Clean Code')).toBeInTheDocument();
  });

  it('shows a pay-on-delivery label for a CREATED order', async () => {
    vi.mocked(orderApi.getOrder).mockResolvedValue({
      id: 42, status: 'CREATED', totalAmount: 35.99, paymentType: 'CASH_ON_DELIVERY',
      items: [], createdAt: '2026-01-01T10:00:00',
    });

    renderAtOrder('42');

    expect(await screen.findByText(/pay on delivery/i)).toBeInTheDocument();
  });

  it('shows a not-found message for a 404', async () => {
    const notFoundError = new AxiosError('Not Found', 'ERR_BAD_REQUEST', undefined, undefined, {
      status: 404, statusText: 'Not Found', headers: {}, config: {} as never,
      data: { message: 'Order not found with id: 999' },
    });
    vi.mocked(orderApi.getOrder).mockRejectedValue(notFoundError);

    renderAtOrder('999');

    expect(await screen.findByText(/couldn't find that order/i)).toBeInTheDocument();
  });

  it('shows a generic error message for any other failure', async () => {
    vi.mocked(orderApi.getOrder).mockRejectedValue(new Error('Network error'));

    renderAtOrder('42');

    expect(await screen.findByText(/something went wrong/i)).toBeInTheDocument();
  });
});
