const REFRESH_TOKEN_KEY = 'bookstore_refresh_token';

/**
 * - Access token: kept in a plain module variable (memory only). It never
 *   touches localStorage, so it can't be read by a follow-up XSS payload
 *   that inspects storage. Cost: it's gone on page reload.
 * - Refresh token: persisted to localStorage so the user isn't forced to
 *   log in again on every reload.
 */
let accessToken: string | null = null;

export const tokenStore = {
  getAccessToken(): string | null {
    return accessToken;
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  setTokens({ accessToken: newAccessToken, refreshToken: newRefreshToken }: {
    accessToken?: string;
    refreshToken?: string;
  }): void {
    accessToken = newAccessToken ?? null;
    if (newRefreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken);
    }
  },

  clear(): void {
    accessToken = null;
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};
