package com.karthik.JavaURL.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsTokensUpToCapacityThenRejects() {
        RateLimiter limiter = new RateLimiter(3, 10);

        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isTrue();
        // Token bucket exhausted (3-token capacity, no refill elapsed in these ticks).
        assertThat(limiter.tryAcquire("client-1")).isFalse();
    }

    @Test
    void clientsAreIsolated() {
        RateLimiter limiter = new RateLimiter(1, 10);

        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isFalse();

        // A different client has its own bucket.
        assertThat(limiter.tryAcquire("client-b")).isTrue();
    }

    @Test
    void refillsTokensOverTime() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(1, 10);

        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isFalse();

        // Wait enough for the refill tick to add a token back.
        Thread.sleep(1100);
        assertThat(limiter.tryAcquire("client-1")).isTrue();
    }
}