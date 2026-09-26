import { apiClient } from './client';

import type { CreateVerificationRequest, VerificationRequest } from '@/types';

export async function getMyVerificationRequests() {
  const response = await apiClient.get<VerificationRequest[]>('/api/verification-requests/me');
  return response.data;
}

export async function createVerificationRequest(payload: CreateVerificationRequest) {
  if (!payload.verificationType) {
    throw new Error('verificationType is required.');
  }

  const response = await apiClient.post<VerificationRequest>('/api/verification-requests', payload);
  return response.data;
}
