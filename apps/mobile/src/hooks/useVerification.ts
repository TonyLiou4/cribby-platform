import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createVerificationRequest,
  getMyVerificationRequests,
} from '@/api/verification';
import type { CreateVerificationRequest } from '@/types';

export function useVerificationRequests() {
  return useQuery({
    queryKey: ['verification-requests'],
    queryFn: getMyVerificationRequests,
  });
}

export function useCreateVerificationRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateVerificationRequest) => createVerificationRequest(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['verification-requests'] });
      void queryClient.invalidateQueries({ queryKey: ['current-user'] });
    },
  });
}
