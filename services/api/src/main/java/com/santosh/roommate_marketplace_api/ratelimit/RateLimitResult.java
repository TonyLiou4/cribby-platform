package com.santosh.roommate_marketplace_api.ratelimit;

public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds
) {
}
