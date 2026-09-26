package com.santosh.roommate_marketplace_api.auth.email.service;

import com.santosh.roommate_marketplace_api.auth.email.dto.EmailVerificationResponse;
import com.santosh.roommate_marketplace_api.auth.email.entity.EmailVerificationToken;
import com.santosh.roommate_marketplace_api.auth.email.repository.EmailVerificationTokenRepository;
import com.santosh.roommate_marketplace_api.common.exception.UnauthorizedException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @Test
    void sendVerificationEmailForUnverifiedUserCreatesHashedTokenAndSendsEmail() {
        EmailVerificationService service = service();
        User user = user("test@example.com", false);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of());
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationResponse response = service.sendVerificationEmail("test@example.com");

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(emailSenderService).sendPlainTextEmail(
                org.mockito.ArgumentMatchers.eq("test@example.com"),
                org.mockito.ArgumentMatchers.eq("Verify your Roommate Marketplace email"),
                org.mockito.ArgumentMatchers.contains("/api/auth/verify-email?token=")
        );

        EmailVerificationToken savedToken = tokenCaptor.getValue();
        assertThat(response.message()).contains("verification email");
        assertThat(savedToken.getTokenHash()).isNotBlank();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void sendVerificationEmailForAlreadyVerifiedUserReturnsSafeSuccessWithoutSending() {
        EmailVerificationService service = service();
        User user = user("verified@example.com", true);

        when(userRepository.findByEmailIgnoreCase("verified@example.com")).thenReturn(Optional.of(user));

        EmailVerificationResponse response = service.sendVerificationEmail("verified@example.com");

        assertThat(response.message()).contains("verification email");
        verify(tokenRepository, never()).save(any());
        verify(emailSenderService, never()).sendPlainTextEmail(any(), any(), any());
    }

    @Test
    void verifyValidTokenMarksUserEmailVerifiedAndTokenUsed() {
        EmailVerificationService service = service();
        User user = user("test@example.com", false);
        EmailVerificationToken token = token(user, service.hash("raw-token"), LocalDateTime.now().plusMinutes(30), null);

        when(tokenRepository.findByTokenHash(service.hash("raw-token"))).thenReturn(Optional.of(token));

        EmailVerificationResponse response = service.verifyEmail("raw-token");

        assertThat(response.message()).isEqualTo("Email verified successfully.");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void invalidExpiredAndUsedTokensFail() {
        EmailVerificationService service = service();

        when(tokenRepository.findByTokenHash(service.hash("missing-token"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail("missing-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired email verification token");

        User user = user("test@example.com", false);
        EmailVerificationToken expiredToken = token(
                user,
                service.hash("expired-token"),
                LocalDateTime.now().minusMinutes(1),
                null
        );
        when(tokenRepository.findByTokenHash(service.hash("expired-token"))).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> service.verifyEmail("expired-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired email verification token");

        EmailVerificationToken usedToken = token(
                user,
                service.hash("used-token"),
                LocalDateTime.now().plusMinutes(30),
                LocalDateTime.now()
        );
        when(tokenRepository.findByTokenHash(service.hash("used-token"))).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> service.verifyEmail("used-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired email verification token");
    }

    @Test
    void resendVerificationEmailMarksPreviousUnusedTokensUsedAndCreatesNewToken() {
        EmailVerificationService service = service();
        User user = user("test@example.com", false);
        EmailVerificationToken oldToken = token(user, "old-hash", LocalDateTime.now().plusMinutes(30), null);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(oldToken));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.resendVerificationEmail("test@example.com");

        assertThat(oldToken.isUsed()).isTrue();
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    private EmailVerificationService service() {
        return new EmailVerificationService(
                userRepository,
                tokenRepository,
                emailSenderService,
                "http://localhost:8080",
                30
        );
    }

    private User user(String email, boolean emailVerified) {
        User user = new User();
        user.setFirstName("Test");
        user.setEmail(email);
        user.setEmailVerified(emailVerified);
        return user;
    }

    private EmailVerificationToken token(
            User user,
            String tokenHash,
            LocalDateTime expiresAt,
            LocalDateTime usedAt
    ) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        return token;
    }
}
