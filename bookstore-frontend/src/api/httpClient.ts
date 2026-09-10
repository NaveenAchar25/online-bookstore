import axios from 'axios';
import { getGuestCartId } from '../lib/guestCartId';
import { tokenStore } from '../lib/tokenStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

/**
 * A second, plain axios instance used only for the refresh call itself.
 * Deliberately has none of httpClient's interceptors — if the refresh
 * call also got the "retry on 401" interceptor, an expired refresh token
 * would recurse into itself.
 */
const refreshClient = axios.create({ baseURL: API_BASE_URL });

export { refreshClient };

export const httpClient = axios.create({ baseURL: API_BASE_URL });


httpClient.interceptors.request.use((config) => {
  config.headers['X-Guest-Cart-Id'] = getGuestCartId();

  const accessToken = tokenStore.getAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

// Concurrent 401s three components fetching at once should trigger
// exactly one refresh call, not three — every caller awaits the same promise.
let inFlightRefresh: Promise<string> | null = null;

const AUTH_ENDPOINTS_EXCLUDED_FROM_REFRESH = ['/auth/login', '/auth/register', '/auth/refresh'];

function refreshAccessToken(): Promise<string> {
  if (!inFlightRefresh) {
    const refreshToken = tokenStore.getRefreshToken();
    if (!refreshToken) {
      return Promise.reject(new Error('No refresh token available'));
    }

    inFlightRefresh = refreshClient
      .post('/auth/refresh', { refreshToken })
      .then(({ data }) => {
        tokenStore.setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
        return data.accessToken as string;
      })
      .finally(() => {
        inFlightRefresh = null;
      });
  }
  return inFlightRefresh;
}

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const isUnauthorized = response?.status === 401;
    const alreadyRetried = config?._retriedAfterRefresh;
    const isAuthEndpoint = AUTH_ENDPOINTS_EXCLUDED_FROM_REFRESH.some((path) => config?.url?.includes(path));

    // A 401 from /auth/login means "wrong credentials" — refreshing and
    // retrying makes no sense there, only on a 401 from an endpoint that
    // required a (now-expired) access token.
    if (!isUnauthorized || alreadyRetried || !config || isAuthEndpoint) {
      return Promise.reject(error);
    }

    config._retriedAfterRefresh = true;

    try {
      const newAccessToken = await refreshAccessToken();
      config.headers.Authorization = `Bearer ${newAccessToken}`;
      return httpClient(config);
    } catch (refreshError) {
      tokenStore.clear();
      return Promise.reject(refreshError);
    }
  }
);
