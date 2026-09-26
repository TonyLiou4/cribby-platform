import {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
  create,
} from 'axios';

import { clearTokens, getTokens, saveTokens } from '@/services/tokenStorage';
import type { AuthResponse } from '@/types';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL;

if (!API_BASE_URL) {
  throw new Error(
    'Missing EXPO_PUBLIC_API_BASE_URL. Add it to your project root .env file and restart Expo with `npx expo start -c`.',
  );
}

if (__DEV__) {
  // TODO: Safe to remove after local API environment setup is confirmed.
  console.log(`API Base URL: ${API_BASE_URL}`);
}

type RetriableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

export const apiClient = create({
  baseURL: API_BASE_URL,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

const refreshClient = create({
  baseURL: API_BASE_URL,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

let refreshPromise: Promise<AuthResponse> | null = null;

async function refreshAuthToken() {
  const { refreshToken } = await getTokens();

  if (!refreshToken) {
    throw new Error('Missing refresh token.');
  }

  refreshPromise ??= refreshClient
    .post<AuthResponse>('/api/auth/refresh', { refreshToken })
    .then(async (response) => {
      await saveTokens(response.data.accessToken, response.data.refreshToken);
      const { useAuthStore } = await import('@/store/authStore');
      useAuthStore.setState((state) => ({
        user: response.data.user ?? state.user,
        accessToken: response.data.accessToken,
        refreshToken: response.data.refreshToken,
        isAuthenticated: true,
      }));
      return response.data;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

apiClient.interceptors.request.use(async (config) => {
  const { accessToken } = await getTokens();

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;

    if (error.response?.status !== 401 || !originalRequest || originalRequest._retry) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      const refreshedAuth = await refreshAuthToken();
      originalRequest.headers.Authorization = `Bearer ${refreshedAuth.accessToken}`;
      return apiClient(originalRequest as AxiosRequestConfig);
    } catch (refreshError) {
      await clearTokens();
      const { useAuthStore } = await import('@/store/authStore');
      useAuthStore.setState({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
      });
      return Promise.reject(refreshError);
    }
  },
);

export async function resetApiAuth() {
  await clearTokens();
}
