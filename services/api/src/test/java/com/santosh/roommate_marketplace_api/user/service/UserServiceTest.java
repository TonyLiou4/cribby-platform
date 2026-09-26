package com.santosh.roommate_marketplace_api.user.service;

import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.user.dto.CurrentUserResponse;
import com.santosh.roommate_marketplace_api.user.dto.PublicUserProfileResponse;
import com.santosh.roommate_marketplace_api.user.dto.UpdateProfileRequest;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUserByEmailReturnsSafeUserData() {
        User user = testUser("test@example.com");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        CurrentUserResponse response = userService.getCurrentUserByEmail("test@example.com");

        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.firstName()).isEqualTo("Test");
    }

    @Test
    void updateCurrentUserProfileUpdatesAuthenticatedUser() {
        User user = testUser("test@example.com");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        CurrentUserResponse response = userService.updateCurrentUserProfile(
                "test@example.com",
                new UpdateProfileRequest(
                        "Updated",
                        "Name",
                        "+1 555 999 0000",
                        "Boston University",
                        "https://example.com/new.jpg",
                        "Updated bio",
                        "OTHER",
                        UserType.ALUMNI
                )
        );

        assertThat(response.firstName()).isEqualTo("Updated");
        assertThat(response.lastName()).isEqualTo("Name");
        assertThat(response.universityName()).isEqualTo("Boston University");
        assertThat(response.userType()).isEqualTo(UserType.ALUMNI);
        assertThat(user.getProfile().getDisplayName()).isEqualTo("Updated Name");
    }

    @Test
    void missingCurrentUserThrowsUsernameNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUserByEmail("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void publicProfileReturnsOnlySafeFields() {
        User user = testUser("test@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PublicUserProfileResponse response = userService.getPublicProfile(1L);

        assertThat(response.displayName()).isEqualTo("Test S.");
        assertThat(response.universityName()).isEqualTo("Northeastern University");
    }

    @Test
    void missingPublicProfileThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPublicProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User testUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setUniversityName("Northeastern University");
        user.setUserType(UserType.STUDENT);

        UserProfile profile = new UserProfile();
        profile.setDisplayName("Test Student");
        user.setProfile(profile);
        return user;
    }
}
