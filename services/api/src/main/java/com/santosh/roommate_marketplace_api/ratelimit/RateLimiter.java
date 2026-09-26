package com.santosh.roommate_marketplace_api.ratelimit;

public interface RateLimiter {

    RateLimitResult tryConsume(String policyName, String subjectKey);
}
