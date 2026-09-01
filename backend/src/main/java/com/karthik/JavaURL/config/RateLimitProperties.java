package com.karthik.JavaURL.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the per-client rate limiter that protects the public write API.
 */
@ConfigurationProperties(prefix = "app.ratelimit")
public record RateLimitProperties(boolean enabled, int capacity, double refillPerSecond) {

    public RateLimitProperties {
        if (capacity <= 0) capacity = 100;
        if (refillPerSecond <= 0) refillPerSecond = 50;
    }
}