import MockAdapter from 'axios-mock-adapter';
import { beforeEach, describe, expect, it } from 'vitest';
import { httpClient, refreshClient } from '../api/httpClient';
import { tokenStore } from '../lib/tokenStore';

describe('httpClient auth behavior', () => {
  let mock: MockAdapter;
  let refreshMock: MockAdapter;

  beforeEach(() => {
    tokenStore.clear();
    localStorage.clear();
    mock = new MockAdapter(httpClient);
    refreshMock = new MockAdapter(refreshClient);
  });

  it('attaches the access token as a Bearer header when present', async () => {
    tokenStore.setTokens({ accessToken: 'my-access-token', refreshToken: 'my-refresh-token' });
    mock.onGet('/cart').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer my-access-token');
      return [200, { items: [], totalAmount: 0 }];
    });

    await httpClient.get('/cart');
  });

  it('sends no Authorization header when there is no access token', async () => {
    mock.onGet('/books').reply((config) => {
      expect(config.headers?.Authorization).toBeUndefined();
      return [200, []];
    });

    await httpClient.get('/books');
  });

  it('on a 401, refreshes the token once and retries the original request', async () => {
    tokenStore.setTokens({ accessToken: 'expired-token', refreshToken: 'valid-refresh-token' });

    let callCount = 0;
    mock.onGet('/cart').reply((config) => {
      callCount += 1;
      if (config.headers?.Authorization === 'Bearer expired-token') {
        return [401, { message: 'Token expired' }];
      }
      return [200, { items: [], totalAmount: 0 }];
    });
    refreshMock.onPost('/auth/refresh').reply(200, {
      accessToken: 'brand-new-token',
      refreshToken: 'new-refresh-token',
    });

    const response = await httpClient.get('/cart');

    expect(response.status).toBe(200);
    expect(callCount).toBe(2); // original failed call + retry
    expect(tokenStore.getAccessToken()).toBe('brand-new-token');
    expect(tokenStore.getRefreshToken()).toBe('new-refresh-token');
  });

  it('clears tokens and rejects when the refresh call itself fails', async () => {
    tokenStore.setTokens({ accessToken: 'expired-token', refreshToken: 'also-invalid' });

    mock.onGet('/cart').reply(401, { message: 'Token expired' });
    refreshMock.onPost('/auth/refresh').reply(401, { message: 'Refresh token invalid' });

    await expect(httpClient.get('/cart')).rejects.toBeTruthy();
    expect(tokenStore.getAccessToken()).toBeNull();
    expect(tokenStore.getRefreshToken()).toBeNull();
  });

  it('does not attempt a refresh on a 401 from the login endpoint itself', async () => {
    mock.onPost('/auth/login').reply(401, { message: 'Invalid email or password' });

    await expect(httpClient.post('/auth/login', {})).rejects.toMatchObject({
      response: { status: 401 },
    });
    expect(refreshMock.history.post).toHaveLength(0);
  });

  it('does not retry a request more than once', async () => {
    tokenStore.setTokens({ accessToken: 'expired-token', refreshToken: 'valid-refresh-token' });

    let callCount = 0;
    mock.onGet('/cart').reply(() => {
      callCount += 1;
      return [401, { message: 'Still expired' }];
    });
    refreshMock.onPost('/auth/refresh').reply(200, {
      accessToken: 'new-token',
      refreshToken: 'new-refresh-token',
    });

    await expect(httpClient.get('/cart')).rejects.toBeTruthy();
    expect(callCount).toBe(2); // original + exactly one retry, never a third attempt
  });
});
