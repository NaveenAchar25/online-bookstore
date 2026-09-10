import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import * as authApi from '../api/authApi';
import { tokenStore } from '../lib/tokenStore';

interface AuthContextValue {
  isAuthenticated: boolean;
  mustChangePassword: boolean;
  error: string | null;
  loginUser: (email: string, password: string) => Promise<void>;
  registerUser: (payload: authApi.RegisterPayload) => Promise<void>;
  logoutUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) {
      return response.data.message;
    }
  }
  return fallback;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // Seeded from whether a refresh token already exists (returning visitor)
  // the access token itself is memory-only and won't have
  // survived a reload, but the presence of a refresh token means the next
  // authenticated request can silently obtain a new one.
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(tokenStore.getRefreshToken()));
  const [mustChangePassword, setMustChangePassword] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loginUser = useCallback(async (email: string, password: string) => {
    setError(null);
    try {
      const response = await authApi.login(email, password);
      setIsAuthenticated(true);
      setMustChangePassword(response.mustChangePassword);
    } catch (err) {
      setError(extractErrorMessage(err, 'Login failed. Please try again.'));
      throw err;
    }
  }, []);

  const registerUser = useCallback(async (payload: authApi.RegisterPayload) => {
    setError(null);
    try {
      await authApi.register(payload);
    } catch (err) {
      setError(extractErrorMessage(err, 'Registration failed. Please try again.'));
      throw err;
    }
  }, []);

  const logoutUser = useCallback(async () => {
    await authApi.logout();
    setIsAuthenticated(false);
    setMustChangePassword(false);
  }, []);

  const value: AuthContextValue = { isAuthenticated, mustChangePassword, error, loginUser, registerUser, logoutUser };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
