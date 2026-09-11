export interface OrderItem {
  bookId: number;
  title: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: number;
  status: 'CREATED' | 'PAID' | 'CANCELLED';
  totalAmount: number;
  paymentType: string;
  items: OrderItem[];
  createdAt: string;
}
