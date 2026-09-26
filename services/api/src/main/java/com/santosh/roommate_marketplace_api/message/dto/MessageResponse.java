package com.santosh.roommate_marketplace_api.message.dto;

import com.santosh.roommate_marketplace_api.message.entity.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getContent(),
                message.isRead(),
                message.getCreatedAt()
        );
    }
}
