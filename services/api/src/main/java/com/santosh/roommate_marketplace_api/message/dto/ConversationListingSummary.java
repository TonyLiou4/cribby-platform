package com.santosh.roommate_marketplace_api.message.dto;

import com.santosh.roommate_marketplace_api.listing.entity.Listing;

public record ConversationListingSummary(
        Long id,
        String title,
        String city,
        String universityName
) {
    public static ConversationListingSummary from(Listing listing) {
        return new ConversationListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getUniversityName()
        );
    }
}
