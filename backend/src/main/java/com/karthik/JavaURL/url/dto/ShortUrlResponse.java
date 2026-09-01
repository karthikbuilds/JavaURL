package com.karthik.JavaURL.url.dto;

import java.time.Instant;

/**
 * Representation of a short URL returned by the API.
 */
public record ShortUrlResponse(
        Long id,
        String shortCode,
        String shortUrl,
        String longUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean active,
        long clickCount
) {
}