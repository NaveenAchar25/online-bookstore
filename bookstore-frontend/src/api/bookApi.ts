import { httpClient } from './httpClient';
import type { Book } from '../types/Book';
import type { PagedResponse } from '../types/Page';

export interface GetBooksParams {
  query?: string;
  page?: number;
  size?: number;
}

export async function getBooks(params: GetBooksParams = {}): Promise<PagedResponse<Book>> {
  const { data } = await httpClient.get<PagedResponse<Book>>('/books', {
    params: {
      q: params.query || undefined, // empty string omitted entirely, not sent as ?q=
      page: params.page,
      size: params.size,
    },
  });
  return data;
}

export async function getBookById(id: number): Promise<Book> {
  const { data } = await httpClient.get<Book>(`/books/${id}`);
  return data;
}
