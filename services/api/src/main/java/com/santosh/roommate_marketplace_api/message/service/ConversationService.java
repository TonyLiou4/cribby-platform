package com.santosh.roommate_marketplace_api.message.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.message.dto.ConversationResponse;
import com.santosh.roommate_marketplace_api.message.dto.MessageResponse;
import com.santosh.roommate_marketplace_api.message.dto.SendMessageRequest;
import com.santosh.roommate_marketplace_api.message.dto.StartConversationRequest;
import com.santosh.roommate_marketplace_api.message.entity.Conversation;
import com.santosh.roommate_marketplace_api.message.entity.Message;
import com.santosh.roommate_marketplace_api.message.repository.ConversationRepository;
import com.santosh.roommate_marketplace_api.message.repository.MessageRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ListingRepository listingRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConversationResponse startConversation(String userEmail, StartConversationRequest request) {
        User sender = getUserByEmail(userEmail);
        Listing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        User receiver = listing.getOwner();

        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("You cannot start a conversation with yourself for your own listing");
        }

        Conversation conversation = conversationRepository
                .findBetweenUsersForListing(listing.getId(), sender.getId(), receiver.getId())
                .orElseGet(() -> {
                    Conversation newConversation = new Conversation();
                    newConversation.setListing(listing);
                    newConversation.setSender(sender);
                    newConversation.setReceiver(receiver);
                    return conversationRepository.save(newConversation);
                });

        Message lastMessage = getLastMessage(conversation);
        return ConversationResponse.from(conversation, sender, lastMessage);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(String userEmail) {
        User user = getUserByEmail(userEmail);
        return conversationRepository.findByParticipantId(user.getId())
                .stream()
                .map(conversation -> ConversationResponse.from(conversation, user, getLastMessage(conversation)))
                .sorted(Comparator.comparing(ConversationResponse::lastUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Conversation conversation = getConversationForParticipant(conversationId, user);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, String userEmail, SendMessageRequest request) {
        User sender = getUserByEmail(userEmail);
        Conversation conversation = getConversationForParticipant(conversationId, sender);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.content().trim());
        message.setRead(false);

        return MessageResponse.from(messageRepository.save(message));
    }

    private Conversation getConversationForParticipant(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!isParticipant(conversation, user)) {
            throw new AccessDeniedException("Only conversation participants can access messages");
        }
        return conversation;
    }

    private boolean isParticipant(Conversation conversation, User user) {
        return conversation.getSender().getId().equals(user.getId())
                || conversation.getReceiver().getId().equals(user.getId());
    }

    private Message getLastMessage(Conversation conversation) {
        return messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
