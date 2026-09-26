package com.santosh.roommate_marketplace_api.user.dto;

import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.VerificationBadge;

import java.util.List;

public record UserProfileResponse(
        Long userId,
        String email,
        String displayName,
        String profilePhotoUrl,
        String university,
        String bio,
        String city,
        String roommatePreferences,
        List<VerificationBadge> verificationBadges
) {
    public static UserProfileResponse from(User user) {
        UserProfile profile = user.getProfile();
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                profile.getDisplayName(),
                profile.getProfilePhotoUrl(),
                profile.getUniversity(),
                profile.getBio(),
                profile.getCity(),
                profile.getRoommatePreferences(),
                user.getVerificationBadges()
        );
    }
}
