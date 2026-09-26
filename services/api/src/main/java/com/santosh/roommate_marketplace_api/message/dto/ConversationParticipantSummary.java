package com.santosh.roommate_marketplace_api.message.dto;

import com.santosh.roommate_marketplace_api.user.entity.User;

public record ConversationParticipantSummary(
        Long id,
        String displayName,
        String profilePhotoUrl
) {
    public static ConversationParticipantSummary from(User user) {
        return new ConversationParticipantSummary(
                user.getId(),
                buildDisplayName(user),
                user.getProfilePhotoUrl()
        );
    }

    private static String buildDisplayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (firstName == null || firstName.isBlank()) {
            return null;
        }
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName.substring(0, 1) + ".";
    }
}
