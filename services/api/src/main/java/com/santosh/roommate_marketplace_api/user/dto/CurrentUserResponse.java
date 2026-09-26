package com.santosh.roommate_marketplace_api.user.dto;

import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.entity.VerificationBadge;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String universityName,
        UserType userType,
        String profilePhotoUrl,
        String bio,
        String gender,
        List<VerificationBadge> verificationBadges
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getUniversityName(),
                user.getUserType(),
                user.getProfilePhotoUrl(),
                user.getBio(),
                user.getGender(),
                user.getVerificationBadges()
        );
    }
}
