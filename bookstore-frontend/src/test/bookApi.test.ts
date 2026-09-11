import MockAdapter from 'axios-mock-adapter';
import { beforeEach, describe, expect, it } from 'vitest';
import * as bookApi from '../api/bookApi';
import { httpClient } from '../api/httpClient';

const samplePage = {
  items: [{ id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10 }],
  page: 0,
  size: 12,
  totalElements: 1,
  totalPages: 1,
};

describe('bookApi', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(httpClient);
  });

  it('getBooks fetches a page of the catalog', async () => {
    mock.onGet('/books').reply(200, samplePage);

    const result = await bookApi.getBooks();

    expect(result.items).toHaveLength(1);
    expect(result.items[0].title).toBe('Clean Code');
    expect(result.totalElements).toBe(1);
  });

  it('getBooks sends page and size as query params', async () => {
    mock.onGet('/books').reply((config) => {
      expect(config.params.page).toBe(2);
      expect(config.params.size).toBe(5);
      return [200, samplePage];
    });

    await bookApi.getBooks({ page: 2, size: 5 });
  });

  it('getBooks sends q when a search query is given', async () => {
    mock.onGet('/books').reply((config) => {
      expect(config.params.q).toBe('clean');
      return [200, samplePage];
    });

    await bookApi.getBooks({ query: 'clean' });
  });

  it('getBooks omits q entirely for an empty query, rather than sending ?q=', async () => {
    mock.onGet('/books').reply((config) => {
      expect(config.params.q).toBeUndefined();
      return [200, samplePage];
    });

    await bookApi.getBooks({ query: '' });
  });

  it('getBookById fetches a single book', async () => {
    mock.onGet('/books/1').reply(200, {
      id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10,
    });

    const book = await bookApi.getBookById(1);

    expect(book.title).toBe('Clean Code');
  });

  it('propagates a 404 when the book does not exist', async () => {
    mock.onGet('/books/999').reply(404, { message: 'Book not found with id: 999' });

    await expect(bookApi.getBookById(999)).rejects.toBeTruthy();
  });
});
