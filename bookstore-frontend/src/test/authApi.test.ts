import MockAdapter from 'axios-mock-adapter';
import { beforeEach, describe, expect, it } from 'vitest';
import * as authApi from '../api/authApi';
import { httpClient } from '../api/httpClient';
import { tokenStore } from '../lib/tokenStore';

describe('authApi', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    tokenStore.clear();
    mock = new MockAdapter(httpClient);
  });

  it('register posts the payload and returns the created user summary', async () => {
    const payload = { email: 'jane@example.com', password: 'StrongPass1!', firstName: 'Jane', lastName: 'Doe' };
    mock.onPost('/auth/register', payload).reply(201, { id: 1, email: 'jane@example.com' });

    const result = await authApi.register(payload);

    expect(result).toEqual({ id: 1, email: 'jane@example.com' });
  });

  it('login stores the returned tokens', async () => {
    mock.onPost('/auth/login').reply(200, {
      accessToken: 'access-token', refreshToken: 'refresh-token',
      tokenType: 'Bearer', expiresIn: 900, mustChangePassword: false,
    });

    await authApi.login('jane@example.com', 'StrongPass1!');

    expect(tokenStore.getAccessToken()).toBe('access-token');
    expect(tokenStore.getRefreshToken()).toBe('refresh-token');
  });

  it('login does not store tokens if the request fails', async () => {
    mock.onPost('/auth/login').reply(401, { message: 'Invalid email or password' });

    await expect(authApi.login('jane@example.com', 'wrong')).rejects.toBeTruthy();

    expect(tokenStore.getAccessToken()).toBeNull();
  });

  it('logout clears local tokens even if the server call fails', async () => {
    tokenStore.setTokens({ accessToken: 'a', refreshToken: 'r' });
    mock.onPost('/auth/logout').reply(500);

    await authApi.logout();

    expect(tokenStore.getAccessToken()).toBeNull();
    expect(tokenStore.getRefreshToken()).toBeNull();
  });

  it('logout is a no-op against the server if there is nothing to revoke', async () => {
    tokenStore.clear();

    await authApi.logout(); // should not throw despite no mocked route

    expect(tokenStore.getRefreshToken()).toBeNull();
  });
});
