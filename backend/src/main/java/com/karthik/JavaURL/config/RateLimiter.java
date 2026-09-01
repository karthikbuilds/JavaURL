package com.karthik.JavaURL.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple thread-safe token-bucket rate limiter keyed by an arbitrary identity
 * (client IP). Implements the classic token bucket algorithm with whole-seconds
 * refill ticks, which is adequate for public URL-shortening workloads.
 */
public class RateLimiter {

    private final int capacity;
    private final double refillPerSecond;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int capacity, double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    /**
     * Attempts to consume one token on behalf of {@code key}.
     *
     * @return {@code true} if allowed, {@code false} if the client is over the limit.
     */
    public boolean tryAcquire(String key) {
        long now = nowSeconds();
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            Bucket b = existing;
            if (b == null) {
                return new Bucket(now, capacity);
            }
            if (now > b.lastRefillSeconds) {
                long elapsed = now - b.lastRefillSeconds;
                long tokens = Math.min(capacity,
                        (long) (b.tokens + elapsed * refillPerSecond));
                b.tokens = tokens;
                b.lastRefillSeconds = now;
            }
            return b;
        });

        return bucket.tryConsume();
    }

    /** Removes tracking state for a key (e.g. when a client disconnects or after a window). */
    public void remove(String key) {
        buckets.remove(key);
    }

    private static long nowSeconds() {
        return System.nanoTime() / 1_000_000_000L;
    }

    private static final class Bucket {
        private long lastRefillSeconds;
        private long tokens;

        Bucket(long nowSeconds, int capacity) {
            this.lastRefillSeconds = nowSeconds;
            this.tokens = capacity;
        }

        synchronized boolean tryConsume() {
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }
    }
}