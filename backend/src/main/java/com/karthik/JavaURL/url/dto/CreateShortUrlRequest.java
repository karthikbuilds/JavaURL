package com.karthik.JavaURL.url.dto;

import com.karthik.JavaURL.url.ValidUrl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request body for creating a short URL.
 *
 * @param longUrl       the destination URL
 * @param customAlias   optional human-chosen alias instead of a generated code
 * @param expiresInDays optional relative expiry in days
 * @param expiresAt     optional absolute expiry instant
 */
public record CreateShortUrlRequest(

        @NotBlank(message = "longUrl is required")
        @Size(max = 2048, message = "longUrl must be at most 2048 characters")
        @ValidUrl
        String longUrl,

        @Size(min = 3, max = 64, message = "customAlias must be between 3 and 64 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "customAlias may only contain letters, digits, '-' and '_'")
        String customAlias,

        @Min(value = 1, message = "expiresInDays must be at least 1")
        @Max(value = 3650, message = "expiresInDays may be at most 3650")
        Integer expiresInDays,

        Instant expiresAt
) {
}