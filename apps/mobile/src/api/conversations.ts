import { apiClient } from './client';

import type { Conversation, Message, StartConversationRequest, SendMessageRequest } from '@/types';

export async function startConversation(payload: StartConversationRequest) {
  const response = await apiClient.post<Conversation>('/api/conversations', payload);
  return response.data;
}

export async function getMyConversations() {
  const response = await apiClient.get<Conversation[]>('/api/conversations/me');
  return response.data;
}

export async function getConversationMessages(conversationId: number) {
  const response = await apiClient.get<Message[]>(`/api/conversations/${conversationId}/messages`);
  return response.data;
}

export async function sendMessage(conversationId: number, payload: SendMessageRequest) {
  const content = payload.content.trim();

  if (!content) {
    throw new Error('Message content is required.');
  }

  const response = await apiClient.post<Message>(
    `/api/conversations/${conversationId}/messages`,
    { content },
  );
  return response.data;
}
