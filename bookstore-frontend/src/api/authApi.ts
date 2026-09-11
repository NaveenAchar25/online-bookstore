import { httpClient } from './httpClient';
import { tokenStore } from '../lib/tokenStore';
import type { AuthResponse, UserSummary } from '../types/Auth';

export interface RegisterPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export async function register(payload: RegisterPayload): Promise<UserSummary> {
  const { data } = await httpClient.post<UserSummary>('/auth/register', payload);
  return data;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await httpClient.post<AuthResponse>('/auth/login', { email, password });
  tokenStore.setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
  return data;
}

export async function logout(): Promise<void> {
  const refreshToken = tokenStore.getRefreshToken();
  tokenStore.clear();

  if (refreshToken) {
    try {
      await httpClient.post('/auth/logout', { refreshToken });
    } catch {
      // Best-effort: local state is already cleared, so a failed server
      // call to revoke the token shouldn't block the user from logging out.
    }
  }
}
