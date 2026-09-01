package com.karthik.JavaURL.url;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A shortened URL. The {@link #shortCode} is the unique, public identifier used
 * both as the redirect path ({@code GET /{shortCode}}) and in API paths.
 */
@Entity
@Table(name = "short_urls", indexes = {
        @Index(name = "idx_short_urls_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 64)
    private String shortCode;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Optional expiry instant; {@code null} means the link never expires. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0L;

    public ShortUrl(String shortCode, String longUrl, Instant expiresAt) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}