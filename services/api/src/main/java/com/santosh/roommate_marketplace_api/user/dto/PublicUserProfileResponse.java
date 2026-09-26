package com.santosh.roommate_marketplace_api.user.dto;

import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.entity.VerificationBadge;

import java.time.LocalDateTime;
import java.util.List;

public record PublicUserProfileResponse(
        Long id,
        String displayName,
        String universityName,
        UserType userType,
        String profilePhotoUrl,
        String bio,
        List<VerificationBadge> verificationBadges,
        LocalDateTime createdAt
) {
    public static PublicUserProfileResponse from(User user) {
        return new PublicUserProfileResponse(
                user.getId(),
                buildDisplayName(user),
                user.getUniversityName(),
                user.getUserType(),
                user.getProfilePhotoUrl(),
                user.getBio(),
                user.getVerificationBadges(),
                user.getCreatedAt()
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
