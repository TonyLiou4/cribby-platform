import { apiClient } from './client';

import type { UpdateProfileRequest, User } from '@/types';

export async function getCurrentUserProfile() {
  const response = await apiClient.get<User>('/api/users/me');
  return response.data;
}

export async function updateProfile(payload: UpdateProfileRequest) {
  const response = await apiClient.put<User>('/api/users/me/profile', payload);
  return response.data;
}

export async function getPublicProfile(userId: number) {
  const response = await apiClient.get<User>(`/api/users/${userId}/public-profile`);
  return response.data;
}
