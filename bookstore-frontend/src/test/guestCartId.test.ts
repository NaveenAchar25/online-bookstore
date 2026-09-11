import { beforeEach, describe, expect, it } from 'vitest';
import { getGuestCartId } from '../lib/guestCartId';

describe('getGuestCartId', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('generates a new id when none exists yet', () => {
    const id = getGuestCartId();

    expect(id).toBeTruthy();
    expect(localStorage.getItem('bookstore_guest_cart_id')).toBe(id);
  });

  it('returns the same id on subsequent calls', () => {
    const first = getGuestCartId();
    const second = getGuestCartId();

    expect(second).toBe(first);
  });

  it('persists across what a page reload would look like (reading straight from localStorage)', () => {
    const id = getGuestCartId();

    // Simulate a reload: nothing in memory survives, only localStorage does.
    expect(localStorage.getItem('bookstore_guest_cart_id')).toBe(id);
  });
});
