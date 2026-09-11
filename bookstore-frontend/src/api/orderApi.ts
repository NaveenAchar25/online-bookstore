import { httpClient } from './httpClient';
import type { Order } from '../types/Order';

export interface CheckoutPayload {
  paymentType: 'CREDIT_CARD' | 'CASH_ON_DELIVERY';
  cardNumber?: string;
  cardExpiry?: string;
}

export async function checkout(payload: CheckoutPayload): Promise<Order> {
  const { data } = await httpClient.post<Order>('/orders', payload);
  return data;
}

export async function getMyOrders(): Promise<Order[]> {
  const { data } = await httpClient.get<Order[]>('/orders');
  return data;
}

export async function getOrder(id: number): Promise<Order> {
  const { data } = await httpClient.get<Order>(`/orders/${id}`);
  return data;
}
