import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import * as cartApi from '../api/cartApi';
import { extractErrorMessage } from '../lib/errorMessage';
import type { Cart } from '../types/Cart';

interface CartContextValue {
  cart: Cart | null;
  error: string | null;
  itemCount: number;
  addItem: (bookId: number, quantity: number) => Promise<void>;
  updateItemQuantity: (bookId: number, quantity: number) => Promise<void>;
  removeItem: (bookId: number) => Promise<void>;
  refresh: () => Promise<void>;
}

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const fetched = await cartApi.getCart();
      setCart(fetched);
    } catch {
      setError("Couldn't load your cart. Please try again shortly.");
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addItem = useCallback(async (bookId: number, quantity: number) => {
    setError(null);
    try {
      const updated = await cartApi.addItem(bookId, quantity);
      setCart(updated);
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not add this book to your cart.'));
      throw err;
    }
  }, []);

  const updateItemQuantity = useCallback(async (bookId: number, quantity: number) => {
    setError(null);
    try {
      const updated = await cartApi.updateItemQuantity(bookId, quantity);
      setCart(updated);
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not update that quantity.'));
      throw err;
    }
  }, []);

  const removeItem = useCallback(async (bookId: number) => {
    setError(null);
    try {
      const updated = await cartApi.removeItem(bookId);
      setCart(updated);
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not remove that item.'));
      throw err;
    }
  }, []);

  const itemCount = cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0;

  // refresh is exposed publicly (not just used internally on mount)
  // specifically for checkout: it clears the cart server-side directly,
  // bypassing addItem/updateItemQuantity/removeItem entirely
  const value: CartContextValue = { cart, error, itemCount, addItem, updateItemQuantity, removeItem, refresh };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextValue {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
