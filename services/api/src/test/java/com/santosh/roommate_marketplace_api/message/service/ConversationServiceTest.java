package com.santosh.roommate_marketplace_api.message.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
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
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void startsNewConversationWithListingOwner() {
        User sender = user(1L, "sender@example.com");
        User owner = user(2L, "owner@example.com");
        Listing listing = listing(10L, owner);

        when(userRepository.findByEmailIgnoreCase("sender@example.com")).thenReturn(Optional.of(sender));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(conversationRepository.findBetweenUsersForListing(10L, 1L, 2L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            setId(conversation, 100L);
            return conversation;
        });
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.empty());

        ConversationResponse response = conversationService.startConversation(
                "sender@example.com",
                new StartConversationRequest(10L)
        );

        assertThat(response.conversationId()).isEqualTo(100L);
        assertThat(response.otherParticipant().id()).isEqualTo(2L);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void returnsExistingConversationInsteadOfCreatingDuplicate() {
        User sender = user(1L, "sender@example.com");
        User owner = user(2L, "owner@example.com");
        Listing listing = listing(10L, owner);
        Conversation existing = conversation(100L, listing, sender, owner);

        when(userRepository.findByEmailIgnoreCase("sender@example.com")).thenReturn(Optional.of(sender));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(conversationRepository.findBetweenUsersForListing(10L, 1L, 2L)).thenReturn(Optional.of(existing));
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.empty());

        ConversationResponse response = conversationService.startConversation(
                "sender@example.com",
                new StartConversationRequest(10L)
        );

        assertThat(response.conversationId()).isEqualTo(100L);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void cannotStartConversationForOwnListing() {
        User owner = user(1L, "owner@example.com");
        Listing listing = listing(10L, owner);

        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> conversationService.startConversation(
                "owner@example.com",
                new StartConversationRequest(10L)
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void onlyParticipantsCanReadMessages() {
        User sender = user(1L, "sender@example.com");
        User receiver = user(2L, "receiver@example.com");
        User other = user(3L, "other@example.com");
        Conversation conversation = conversation(100L, listing(10L, receiver), sender, receiver);

        when(userRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(other));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.getMessages(100L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void participantCanSendMessage() {
        User sender = user(1L, "sender@example.com");
        User receiver = user(2L, "receiver@example.com");
        Conversation conversation = conversation(100L, listing(10L, receiver), sender, receiver);

        when(userRepository.findByEmailIgnoreCase("sender@example.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            setId(message, 200L);
            return message;
        });

        MessageResponse response = conversationService.sendMessage(
                100L,
                "sender@example.com",
                new SendMessageRequest("Hello, is this room still available?")
        );

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.content()).isEqualTo("Hello, is this room still available?");
    }

    @Test
    void messagesAreReturnedAscending() {
        User sender = user(1L, "sender@example.com");
        User receiver = user(2L, "receiver@example.com");
        Conversation conversation = conversation(100L, listing(10L, receiver), sender, receiver);
        Message first = message(201L, conversation, sender, "First");
        Message second = message(202L, conversation, receiver, "Second");

        when(userRepository.findByEmailIgnoreCase("sender@example.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(first, second));

        List<MessageResponse> response = conversationService.getMessages(100L, "sender@example.com");

        assertThat(response).extracting(MessageResponse::content).containsExactly("First", "Second");
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);
        setId(user, id);
        return user;
    }

    private Listing listing(Long id, User owner) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room");
        listing.setDescription("Clean private room.");
        listing.setCity("Boston");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(ListingStatus.AVAILABLE);
        setId(listing, id);
        return listing;
    }

    private Conversation conversation(Long id, Listing listing, User sender, User receiver) {
        Conversation conversation = new Conversation();
        conversation.setListing(listing);
        conversation.setSender(sender);
        conversation.setReceiver(receiver);
        setId(conversation, id);
        return conversation;
    }

    private Message message(Long id, Conversation conversation, User sender, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        setId(message, id);
        return message;
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = com.santosh.roommate_marketplace_api.common.entity.BaseEntity.class
                    .getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
