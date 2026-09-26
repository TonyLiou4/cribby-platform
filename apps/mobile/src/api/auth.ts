import { apiClient } from './client';

import type { AuthResponse, LoginRequest, RegisterRequest, User } from '@/types';

type EmailRequest = {
  email: string;
};

type RefreshTokenRequest = {
  refreshToken: string;
};

export async function login(payload: LoginRequest) {
  const response = await apiClient.post<AuthResponse>('/api/auth/login', payload);
  return response.data;
}

export async function register(payload: RegisterRequest) {
  const response = await apiClient.post<AuthResponse>('/api/auth/register', payload);
  return response.data;
}

export async function refreshToken(payload: RefreshTokenRequest) {
  const response = await apiClient.post<AuthResponse>('/api/auth/refresh', payload);
  return response.data;
}

export async function logout(refreshToken?: string | null) {
  await apiClient.post('/api/auth/logout', refreshToken ? { refreshToken } : undefined);
}

export async function getCurrentUser() {
  const response = await apiClient.get<User>('/api/users/me');
  return response.data;
}

export async function sendVerificationEmail(payload: EmailRequest) {
  const response = await apiClient.post('/api/auth/send-verification-email', payload);
  return response.data;
}

export async function resendVerificationEmail(payload: EmailRequest) {
  const response = await apiClient.post('/api/auth/resend-verification-email', payload);
  return response.data;
}

export async function verifyEmail(token: string) {
  const response = await apiClient.get('/api/auth/verify-email', {
    params: { token },
  });
  return response.data;
}
