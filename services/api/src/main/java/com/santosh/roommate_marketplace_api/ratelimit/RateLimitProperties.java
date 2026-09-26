package com.santosh.roommate_marketplace_api.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Map<String, Limit> policies = defaultPolicies();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Limit> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, Limit> policies) {
        this.policies = policies;
    }

    public Limit getPolicy(String name) {
        Limit limit = policies.get(name);
        if (limit == null) {
            throw new IllegalStateException("Missing rate limit policy: " + name);
        }
        return limit;
    }

    private static Map<String, Limit> defaultPolicies() {
        Map<String, Limit> defaults = new LinkedHashMap<>();
        defaults.put("login", new Limit(5, Duration.ofMinutes(1)));
        defaults.put("register", new Limit(3, Duration.ofHours(1)));
        defaults.put("refresh", new Limit(20, Duration.ofMinutes(1)));
        defaults.put("verification-email", new Limit(3, Duration.ofHours(1)));
        defaults.put("reports", new Limit(5, Duration.ofHours(1)));
        defaults.put("conversations", new Limit(10, Duration.ofDays(1)));
        defaults.put("messages", new Limit(30, Duration.ofMinutes(1)));
        return defaults;
    }

    public static class Limit {

        private int capacity;
        private Duration refillPeriod;

        public Limit() {
        }

        public Limit(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }
    }
}
