import MockAdapter from 'axios-mock-adapter';
import { beforeEach, describe, expect, it } from 'vitest';
import * as orderApi from '../api/orderApi';
import { httpClient } from '../api/httpClient';

const sampleOrder = {
  id: 1, status: 'CREATED', totalAmount: 35.99, paymentType: 'CASH_ON_DELIVERY',
  items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 1, subtotal: 35.99 }],
  createdAt: '2026-01-01T10:00:00',
};

describe('orderApi', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(httpClient);
  });

  it('checkout posts the payload and returns the created order', async () => {
    mock.onPost('/orders', { paymentType: 'CASH_ON_DELIVERY' }).reply(201, sampleOrder);

    const order = await orderApi.checkout({ paymentType: 'CASH_ON_DELIVERY' });

    expect(order.id).toBe(1);
    expect(order.status).toBe('CREATED');
  });

  it('checkout includes card fields for a credit card payment', async () => {
    const payload = { paymentType: 'CREDIT_CARD' as const, cardNumber: '4111111111111111', cardExpiry: '12/28' };
    mock.onPost('/orders', payload).reply(201, { ...sampleOrder, status: 'PAID', paymentType: 'CREDIT_CARD' });

    const order = await orderApi.checkout(payload);

    expect(order.status).toBe('PAID');
  });

  it('checkout propagates a 400 for an empty cart', async () => {
    mock.onPost('/orders').reply(400, { message: 'Cannot check out an empty cart' });

    await expect(orderApi.checkout({ paymentType: 'CASH_ON_DELIVERY' })).rejects.toBeTruthy();
  });

  it('getMyOrders fetches the order list', async () => {
    mock.onGet('/orders').reply(200, [sampleOrder]);

    const orders = await orderApi.getMyOrders();

    expect(orders).toHaveLength(1);
  });

  it('getOrder fetches a single order by id', async () => {
    mock.onGet('/orders/1').reply(200, sampleOrder);

    const order = await orderApi.getOrder(1);

    expect(order.id).toBe(1);
  });

  it("getOrder propagates a 404 for someone else's order", async () => {
    mock.onGet('/orders/999').reply(404, { message: 'Order not found with id: 999' });

    await expect(orderApi.getOrder(999)).rejects.toBeTruthy();
  });
});
