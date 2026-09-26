export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  read: boolean;
  createdAt: string;
}

export interface ConversationParticipant {
  id: number;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
}

export interface Conversation {
  conversationId: number;
  listingId?: number;
  listing?: {
    id: number;
    title: string;
  } | null;
  otherParticipant?: ConversationParticipant | null;
  lastMessagePreview?: string | null;
  lastUpdatedAt?: string | null;
  createdAt?: string | null;
}

export interface StartConversationRequest {
  listingId: number;
}

export interface SendMessageRequest {
  content: string;
}
