package com.santosh.roommate_marketplace_api.auth.email.service;

import com.santosh.roommate_marketplace_api.auth.email.dto.EmailVerificationResponse;
import com.santosh.roommate_marketplace_api.auth.email.entity.EmailVerificationToken;
import com.santosh.roommate_marketplace_api.auth.email.repository.EmailVerificationTokenRepository;
import com.santosh.roommate_marketplace_api.common.exception.UnauthorizedException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class EmailVerificationService {

    private static final int TOKEN_BYTES = 64;
    private static final String GENERIC_SEND_MESSAGE = "If your account needs verification, a verification email will be sent.";
    private static final String VERIFIED_MESSAGE = "Email verified successfully.";

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailSenderService emailSenderService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String appBaseUrl;
    private final long tokenMinutes;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            EmailSenderService emailSenderService,
            @Value("${app.base-url}") String appBaseUrl,
            @Value("${email.verification.token-minutes:30}") long tokenMinutes
    ) {
        if (tokenMinutes <= 0) {
            throw new IllegalStateException("EMAIL_VERIFICATION_TOKEN_MINUTES must be greater than zero");
        }
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSenderService = emailSenderService;
        this.appBaseUrl = appBaseUrl;
        this.tokenMinutes = tokenMinutes;
    }

    @Transactional
    public EmailVerificationResponse sendVerificationEmail(String userEmail) {
        User user = findUser(userEmail);
        sendVerificationEmailIfNeeded(user);
        return new EmailVerificationResponse(GENERIC_SEND_MESSAGE);
    }

    @Transactional
    public EmailVerificationResponse resendVerificationEmail(String userEmail) {
        User user = findUser(userEmail);
        sendVerificationEmailIfNeeded(user);
        return new EmailVerificationResponse(GENERIC_SEND_MESSAGE);
    }

    @Transactional
    public EmailVerificationResponse verifyEmail(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired email verification token"));

        if (!token.isActive()) {
            throw new UnauthorizedException("Invalid or expired email verification token");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        token.markUsed();

        return new EmailVerificationResponse(VERIFIED_MESSAGE);
    }

    String hash(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new UnauthorizedException("Invalid or expired email verification token");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private void sendVerificationEmailIfNeeded(User user) {
        if (user.isEmailVerified()) {
            return;
        }

        expireExistingUnusedTokens(user);

        String rawToken = generateSecureToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenMinutes));
        tokenRepository.save(token);

        String verificationLink = UriComponentsBuilder
                .fromHttpUrl(appBaseUrl)
                .path("/api/auth/verify-email")
                .queryParam("token", rawToken)
                .toUriString();

        String body = """
                Hi %s,

                Please verify your Roommate Marketplace email by opening this link:

                %s

                This link expires in %d minutes.
                If you did not request this, you can ignore this email.
                """.formatted(displayName(user), verificationLink, tokenMinutes);

        emailSenderService.sendPlainTextEmail(
                user.getEmail(),
                "Verify your Roommate Marketplace email",
                body
        );
    }

    private void expireExistingUnusedTokens(User user) {
        tokenRepository.findByUserAndUsedAtIsNull(user)
                .forEach(EmailVerificationToken::markUsed);
    }

    private User findUser(String userEmail) {
        return userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String displayName(User user) {
        if (StringUtils.hasText(user.getFirstName())) {
            return user.getFirstName().trim();
        }
        return "there";
    }
}
