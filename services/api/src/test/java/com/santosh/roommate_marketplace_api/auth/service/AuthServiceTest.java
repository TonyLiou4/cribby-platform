package com.santosh.roommate_marketplace_api.auth.service;

import com.santosh.roommate_marketplace_api.auth.dto.AuthResponse;
import com.santosh.roommate_marketplace_api.auth.dto.LoginRequest;
import com.santosh.roommate_marketplace_api.auth.dto.LogoutRequest;
import com.santosh.roommate_marketplace_api.auth.dto.RefreshTokenRequest;
import com.santosh.roommate_marketplace_api.auth.dto.RegisterRequest;
import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.security.JwtService;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserWithHashedPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest(
                "Test",
                "Student",
                "TEST@example.com",
                "Password123!",
                "+1 555 123 4567",
                "Northeastern University",
                "Bio",
                "OTHER",
                UserType.STUDENT
        );

        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(900L);
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    @Test
    void registerDuplicateEmailThrowsBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "Test",
                "Student",
                "test@example.com",
                "Password123!",
                null,
                null,
                null,
                null,
                UserType.STUDENT
        );

        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "Password123!"));

        verify(authenticationManager).authenticate(any());
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    @Test
    void refreshRotatesRefreshTokenAndReturnsNewAccessToken() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);

        when(refreshTokenService.validateAndRotate("old-refresh-token")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("new-jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old-refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-jwt-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(900L);
    }

    @Test
    void logoutRevokesRefreshToken() {
        authService.logout(new LogoutRequest("refresh-token"));

        verify(refreshTokenService).revoke("refresh-token");
    }

    @Test
    void loginWithWrongPasswordFails() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
