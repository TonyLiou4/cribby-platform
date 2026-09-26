package com.santosh.roommate_marketplace_api.message.dto;

import com.santosh.roommate_marketplace_api.message.entity.Conversation;
import com.santosh.roommate_marketplace_api.message.entity.Message;
import com.santosh.roommate_marketplace_api.user.entity.User;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long conversationId,
        ConversationListingSummary listing,
        ConversationParticipantSummary otherParticipant,
        String lastMessagePreview,
        LocalDateTime lastUpdatedAt
) {
    public static ConversationResponse from(Conversation conversation, User currentUser, Message lastMessage) {
        return new ConversationResponse(
                conversation.getId(),
                ConversationListingSummary.from(conversation.getListing()),
                ConversationParticipantSummary.from(otherParticipant(conversation, currentUser)),
                lastMessage == null ? null : preview(lastMessage.getContent()),
                lastMessage == null ? conversation.getCreatedAt() : lastMessage.getCreatedAt()
        );
    }

    private static User otherParticipant(Conversation conversation, User currentUser) {
        if (conversation.getSender().getId().equals(currentUser.getId())) {
            return conversation.getReceiver();
        }
        return conversation.getSender();
    }

    private static String preview(String content) {
        if (content == null || content.length() <= 120) {
            return content;
        }
        return content.substring(0, 120);
    }
}
