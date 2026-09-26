import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  getConversationMessages,
  getMyConversations,
  sendMessage,
  startConversation,
} from '@/api/conversations';
import type { SendMessageRequest, StartConversationRequest } from '@/types';

export function useConversations() {
  return useQuery({
    queryKey: ['conversations'],
    queryFn: getMyConversations,
  });
}

export function useConversationMessages(conversationId?: number) {
  return useQuery({
    enabled: Boolean(conversationId),
    queryKey: ['conversation-messages', conversationId],
    queryFn: () => getConversationMessages(conversationId as number),
  });
}

export function useStartConversation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: StartConversationRequest) => startConversation(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
}

export function useSendMessage(conversationId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: SendMessageRequest) => sendMessage(conversationId, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['conversation-messages', conversationId] });
      void queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
}
