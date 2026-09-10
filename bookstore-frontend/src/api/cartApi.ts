import { Cart } from "../types/Cart";
import { httpClient } from "./httpClient";


export async function getCart(): Promise<Cart> {
  const { data } = await httpClient.get<Cart>('/cart');
  return data;
}


export async function addItem(bookId: number, quantity: number): Promise<Cart> {
  const { data } = await httpClient.post<Cart>('/cart/items', { bookId, quantity });
  return data;
}

export async function updateItemQuantity(bookId: number, quantity: number): Promise<Cart> {
  const { data } = await httpClient.put<Cart>(`/cart/items/${bookId}`, { quantity });
  return data;
}

export async function removeItem(bookId: number): Promise<Cart> {
  const { data } = await httpClient.delete<Cart>(`/cart/items/${bookId}`);
  return data;
}
