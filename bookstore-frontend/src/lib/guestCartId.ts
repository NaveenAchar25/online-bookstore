const STORAGE_KEY = 'bookstore_guest_cart_id';

/**
 * Returns this browser's guest cart id, generating and persisting one on
 * first call if none exists yet. This is what lets a guest's cart survive
 * a page reload without ever requiring an account it's purely a client-generated, opaque key.
 */
export function getGuestCartId(): string {
  const existing = localStorage.getItem(STORAGE_KEY);
  if (existing) {
    return existing;
  }

  const generated = crypto.randomUUID();
  localStorage.setItem(STORAGE_KEY, generated);
  return generated;
}
