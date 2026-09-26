import type { Conversation } from '@/types';

export function getConversationId(conversation: Conversation) {
  return conversation.conversationId;
}
