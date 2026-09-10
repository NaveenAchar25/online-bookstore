import axios from 'axios';
import { getGuestCartId } from '../lib/guestCartId';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const httpClient = axios.create({ baseURL: API_BASE_URL });

// Every request carries the guest cart id — harmless for endpoints that
// don't need it (books), required for the cart endpoints. Attaching it
// globally here means individual API call sites never have to remember to.
httpClient.interceptors.request.use((config) => {
  config.headers['X-Guest-Cart-Id'] = getGuestCartId();
  return config;
});
