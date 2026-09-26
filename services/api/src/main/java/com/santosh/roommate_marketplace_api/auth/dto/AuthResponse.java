package com.santosh.roommate_marketplace_api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.santosh.roommate_marketplace_api.user.dto.CurrentUserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        CurrentUserResponse user
) {
    public AuthResponse(String accessToken, String tokenType, CurrentUserResponse user) {
        this(accessToken, null, tokenType, 0, user);
    }

    @JsonProperty("token")
    public String token() {
        return accessToken;
    }
}
