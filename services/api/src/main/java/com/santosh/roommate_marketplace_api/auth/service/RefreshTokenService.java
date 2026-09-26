package com.santosh.roommate_marketplace_api.auth.service;

import com.santosh.roommate_marketplace_api.auth.entity.RefreshToken;
import com.santosh.roommate_marketplace_api.auth.repository.RefreshTokenRepository;
import com.santosh.roommate_marketplace_api.common.exception.UnauthorizedException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        if (refreshTokenExpirationMs <= 0) {
            throw new IllegalStateException("JWT_REFRESH_TOKEN_EXPIRATION_MS must be greater than zero");
        }
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateSecureToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000));

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public User validateAndRotate(String rawToken) {
        RefreshToken refreshToken = findValidToken(rawToken);
        User user = refreshToken.getUser();
        refreshToken.revoke();
        return user;
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshToken refreshToken = findValidToken(rawToken);
        refreshToken.revoke();
    }

    String hash(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private RefreshToken findValidToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isActive()) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        return refreshToken;
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
