import MockAdapter from 'axios-mock-adapter';
import { beforeEach, describe, expect, it } from 'vitest';
import * as bookApi from '../api/bookApi';
import { httpClient } from '../api/httpClient';

describe('bookApi', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(httpClient);
  });

  it('getBooks fetches the catalog', async () => {
    mock.onGet('/books').reply(200, [
      { id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 35.99, stockQuantity: 10 },
    ]);

    const books = await bookApi.getBooks();

    expect(books).toHaveLength(1);
    expect(books[0].title).toBe('Clean Code');
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
