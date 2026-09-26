package com.santosh.roommate_marketplace_api.message.controller;

import com.santosh.roommate_marketplace_api.message.dto.ConversationResponse;
import com.santosh.roommate_marketplace_api.message.dto.MessageResponse;
import com.santosh.roommate_marketplace_api.message.dto.SendMessageRequest;
import com.santosh.roommate_marketplace_api.message.dto.StartConversationRequest;
import com.santosh.roommate_marketplace_api.message.service.ConversationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "REST-based conversations and messages")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse startConversation(
            Authentication authentication,
            @Valid @RequestBody StartConversationRequest request
    ) {
        return conversationService.startConversation(authentication.getName(), request);
    }

    @GetMapping("/me")
    public List<ConversationResponse> getMyConversations(Authentication authentication) {
        return conversationService.getMyConversations(authentication.getName());
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return conversationService.getMessages(conversationId, authentication.getName());
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @PathVariable Long conversationId,
            Authentication authentication,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return conversationService.sendMessage(conversationId, authentication.getName(), request);
    }
}
