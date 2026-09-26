package com.santosh.roommate_marketplace_api.auth.service;

import com.santosh.roommate_marketplace_api.auth.entity.RefreshToken;
import com.santosh.roommate_marketplace_api.auth.repository.RefreshTokenRepository;
import com.santosh.roommate_marketplace_api.common.exception.UnauthorizedException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void createRefreshTokenStoresOnlyHashedToken() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 604800000);
        User user = new User();
        user.setEmail("test@example.com");

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = service.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(rawToken).isNotBlank();
        assertThat(savedToken.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void validateAndRotateReturnsUserAndRevokesToken() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 604800000);
        User user = new User();
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(service.hash("raw-refresh-token"));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByTokenHash(service.hash("raw-refresh-token")))
                .thenReturn(Optional.of(refreshToken));

        User result = service.validateAndRotate("raw-refresh-token");

        assertThat(result).isEqualTo(user);
        assertThat(refreshToken.isRevoked()).isTrue();
    }

    @Test
    void invalidExpiredOrRevokedTokenFails() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 604800000);

        when(refreshTokenRepository.findByTokenHash(service.hash("missing-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndRotate("missing-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(new User());
        expiredToken.setTokenHash(service.hash("expired-token"));
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByTokenHash(service.hash("expired-token")))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> service.validateAndRotate("expired-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
