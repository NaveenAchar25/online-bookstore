export interface CartItem {
  bookId: number;
  title: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface Cart {
  items: CartItem[];
  totalAmount: number;
}
