import { beforeEach, describe, expect, it } from 'vitest';
import { tokenStore } from '../lib/tokenStore';

describe('tokenStore', () => {
  beforeEach(() => {
    tokenStore.clear();
  });

  it('starts with no tokens', () => {
    expect(tokenStore.getAccessToken()).toBeNull();
    expect(tokenStore.getRefreshToken()).toBeNull();
  });

  it('setTokens stores both tokens', () => {
    tokenStore.setTokens({ accessToken: 'access-1', refreshToken: 'refresh-1' });

    expect(tokenStore.getAccessToken()).toBe('access-1');
    expect(tokenStore.getRefreshToken()).toBe('refresh-1');
  });

  it('refresh token is persisted to localStorage (survives what a reload would look like)', () => {
    tokenStore.setTokens({ accessToken: 'access-1', refreshToken: 'refresh-1' });

    expect(localStorage.getItem('bookstore_refresh_token')).toBe('refresh-1');
  });

  it('clear removes both tokens', () => {
    tokenStore.setTokens({ accessToken: 'access-1', refreshToken: 'refresh-1' });

    tokenStore.clear();

    expect(tokenStore.getAccessToken()).toBeNull();
    expect(tokenStore.getRefreshToken()).toBeNull();
  });
});
