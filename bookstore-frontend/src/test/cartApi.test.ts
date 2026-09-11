import MockAdapter from 'axios-mock-adapter';
import { beforeEach, describe, expect, it } from 'vitest';
import * as cartApi from '../api/cartApi';
import { httpClient } from '../api/httpClient';

describe('cartApi', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    localStorage.clear();
    mock = new MockAdapter(httpClient);
  });

  it('getCart fetches the current cart', async () => {
    mock.onGet('/cart').reply(200, { items: [], totalAmount: 0 });

    const cart = await cartApi.getCart();

    expect(cart.items).toEqual([]);
  });

  it('every request carries the X-Guest-Cart-Id header', async () => {
    mock.onGet('/cart').reply((config) => {
      expect(config.headers?.['X-Guest-Cart-Id']).toBeTruthy();
      return [200, { items: [], totalAmount: 0 }];
    });

    await cartApi.getCart();
  });

  it('addItem posts the book id and quantity', async () => {
    mock.onPost('/cart/items', { bookId: 1, quantity: 2 }).reply(200, {
      items: [{ bookId: 1, title: 'Clean Code', unitPrice: 35.99, quantity: 2, subtotal: 71.98 }],
      totalAmount: 71.98,
    });

    const cart = await cartApi.addItem(1, 2);

    expect(cart.items).toHaveLength(1);
    expect(cart.totalAmount).toBe(71.98);
  });

  it('updateItemQuantity sends a PUT to the correct book', async () => {
    mock.onPut('/cart/items/1', { quantity: 5 }).reply(200, { items: [], totalAmount: 0 });

    await cartApi.updateItemQuantity(1, 5);

    expect(mock.history.put).toHaveLength(1);
  });

  it('removeItem sends a DELETE to the correct book', async () => {
    mock.onDelete('/cart/items/1').reply(200, { items: [], totalAmount: 0 });

    await cartApi.removeItem(1);

    expect(mock.history.delete).toHaveLength(1);
  });
});
