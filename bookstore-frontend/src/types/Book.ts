/**
 * Mirrors the backend's BookDto exactly — id, title, author, price,
 * stockQuantity are always present; isbn and description are optional
 * because the backend entity allows them to be null.
 */
export interface Book {
  id: number;
  title: string;
  author: string;
  price: number;
  stockQuantity: number;
  isbn?: string;
  description?: string;
}
