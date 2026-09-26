package com.santosh.roommate_marketplace_api.user.service;

import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.user.dto.CurrentUserResponse;
import com.santosh.roommate_marketplace_api.user.dto.PublicUserProfileResponse;
import com.santosh.roommate_marketplace_api.user.dto.UpdateProfileRequest;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return CurrentUserResponse.from(user);
    }

    @Transactional
    public CurrentUserResponse updateCurrentUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (request.firstName() != null) {
            user.setFirstName(trimToNull(request.firstName()));
        }
        if (request.lastName() != null) {
            user.setLastName(trimToNull(request.lastName()));
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(trimToNull(request.phoneNumber()));
        }
        if (request.universityName() != null) {
            user.setUniversityName(trimToNull(request.universityName()));
        }
        if (request.profilePhotoUrl() != null) {
            user.setProfilePhotoUrl(trimToNull(request.profilePhotoUrl()));
        }
        if (request.bio() != null) {
            user.setBio(trimToNull(request.bio()));
        }
        if (request.gender() != null) {
            user.setGender(trimToNull(request.gender()));
        }
        if (request.userType() != null) {
            user.setUserType(request.userType());
        }

        syncUserProfile(user);
        return CurrentUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PublicUserProfileResponse getPublicProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        return PublicUserProfileResponse.from(user);
    }

    private void syncUserProfile(User user) {
        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            user.setProfile(profile);
        }

        profile.setDisplayName(buildDisplayName(user));
        profile.setUniversity(user.getUniversityName());
        profile.setProfilePhotoUrl(user.getProfilePhotoUrl());
        profile.setBio(user.getBio());
    }

    private String buildDisplayName(User user) {
        String firstName = trimToNull(user.getFirstName());
        String lastName = trimToNull(user.getLastName());

        if (firstName == null && lastName == null) {
            return "Roommate Marketplace User";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
