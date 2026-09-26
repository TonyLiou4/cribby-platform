package com.santosh.roommate_marketplace_api.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class InMemoryRateLimiter implements RateLimiter {

    private final RateLimitProperties properties;
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public RateLimitResult tryConsume(String policyName, String subjectKey) {
        RateLimitProperties.Limit limit = properties.getPolicy(policyName);
        String bucketKey = policyName + ":" + subjectKey;
        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, key -> new TokenBucket(limit));
        return bucket.tryConsume();
    }

    private static final class TokenBucket {

        private final long capacity;
        private final long refillPeriodNanos;
        private double tokens;
        private long lastRefillNanos;

        private TokenBucket(RateLimitProperties.Limit limit) {
            if (limit.getCapacity() <= 0) {
                throw new IllegalStateException("Rate limit capacity must be greater than zero");
            }
            Duration refillPeriod = limit.getRefillPeriod();
            if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
                throw new IllegalStateException("Rate limit refill period must be greater than zero");
            }
            this.capacity = limit.getCapacity();
            this.refillPeriodNanos = refillPeriod.toNanos();
            this.tokens = limit.getCapacity();
            this.lastRefillNanos = System.nanoTime();
        }

        private synchronized RateLimitResult tryConsume() {
            refill();
            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                return new RateLimitResult(true, (long) Math.floor(tokens), 0);
            }

            long retryAfterNanos = (long) Math.ceil((1.0d - tokens) * refillPeriodNanos / capacity);
            long retryAfterSeconds = Math.max(1, (long) Math.ceil(retryAfterNanos / 1_000_000_000.0d));
            return new RateLimitResult(false, 0, retryAfterSeconds);
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillNanos;
            if (elapsedNanos <= 0) {
                return;
            }

            double refillTokens = (double) elapsedNanos * capacity / refillPeriodNanos;
            tokens = Math.min(capacity, tokens + refillTokens);
            lastRefillNanos = now;
        }
    }
}
