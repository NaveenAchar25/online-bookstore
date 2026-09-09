import { httpClient } from './httpClient';
import type { Book } from '../types/Book';

export async function getBooks(): Promise<Book[]> {
  const { data } = await httpClient.get<Book[]>('/books');
  return data;
}

export async function getBookById(id: number): Promise<Book> {
  const { data } = await httpClient.get<Book>(`/books/${id}`);
  return data;
}
