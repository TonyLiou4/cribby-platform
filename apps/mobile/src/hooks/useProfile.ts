import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { sendVerificationEmail } from '@/api/auth';
import { getCurrentUserProfile, updateProfile } from '@/api/profile';
import { useAuthStore } from '@/store';
import type { UpdateProfileRequest } from '@/types';

export function useCurrentUser() {
  return useQuery({
    queryKey: ['current-user'],
    queryFn: getCurrentUserProfile,
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateProfileRequest) => updateProfile(payload),
    onSuccess: (user) => {
      queryClient.setQueryData(['current-user'], user);
      useAuthStore.setState({ user });
    },
  });
}

export function useSendVerificationEmail() {
  return useMutation({
    mutationFn: (email: string) => sendVerificationEmail({ email }),
  });
}
