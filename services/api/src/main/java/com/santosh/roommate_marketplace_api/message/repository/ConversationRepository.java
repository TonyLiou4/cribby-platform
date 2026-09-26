package com.santosh.roommate_marketplace_api.message.repository;

import com.santosh.roommate_marketplace_api.message.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("""
            select conversation from Conversation conversation
            where conversation.listing.id = :listingId
              and (
                    (conversation.sender.id = :firstUserId and conversation.receiver.id = :secondUserId)
                    or
                    (conversation.sender.id = :secondUserId and conversation.receiver.id = :firstUserId)
              )
            """)
    Optional<Conversation> findBetweenUsersForListing(
            @Param("listingId") Long listingId,
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId
    );

    @Query("""
            select conversation from Conversation conversation
            where conversation.sender.id = :userId or conversation.receiver.id = :userId
            """)
    List<Conversation> findByParticipantId(@Param("userId") Long userId);
}
