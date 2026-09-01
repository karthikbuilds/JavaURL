package com.karthik.JavaURL.url;

import com.karthik.JavaURL.analytics.ClickAnalyticsPublisher;
import com.karthik.JavaURL.config.AppProperties;
import com.karthik.JavaURL.util.Base62Codec;
import com.karthik.JavaURL.url.dto.CreateShortUrlRequest;
import com.karthik.JavaURL.url.dto.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    /** Aliases that would shadow built-in routes or infrastructure endpoints. */
    static final Set<String> RESERVED_ALIASES = Set.of("api", "ws", "actuator", "error");

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final ShortUrlRepository repository;
    private final AppProperties properties;
    private final ClickAnalyticsPublisher analyticsPublisher;
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates a new short URL using either the requested custom alias or a freshly
     * generated random Base62 code.
     */
    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        Instant now = Instant.now();
        Instant expiresAt = computeExpiry(request, now);

        String code;
        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            code = request.customAlias();
            if (RESERVED_ALIASES.contains(code.toLowerCase(Locale.ROOT))) {
                throw new AliasAlreadyInUseException(code);
            }
            if (repository.existsByShortCode(code)) {
                throw new AliasAlreadyInUseException(code);
            }
        } else {
            code = generateUniqueCode();
        }

        ShortUrl entity = new ShortUrl(code, request.longUrl().trim(), expiresAt);
        try {
            entity = repository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // Lost a race against a concurrent request claiming the same code.
            throw new AliasAlreadyInUseException(code);
        }
        log.info("Created short link '{}' -> {} (expiresAt={})", code, entity.getLongUrl(), expiresAt);
        return toResponse(entity);
    }

    /**
     * Resolves a short code for redirection: validates availability/expiry,
     * increments the click counter and broadcasts the new count over WebSocket.
     */
    @Transactional
    public ShortUrlResponse resolveAndCount(String code) {
        ShortUrl entity = findEntity(code);
        if (!entity.isActive()) {
            throw new UrlNotAvailableException(code, "it was deactivated");
        }
        if (entity.isExpired(Instant.now())) {
            entity.setActive(false); // lazily mark expired links as inactive
            log.debug("Short link '{}' has expired; marking inactive", code);
            throw new UrlNotAvailableException(code, "it expired");
        }
        entity.setClickCount(entity.getClickCount() + 1);
        entity = repository.save(entity);
        analyticsPublisher.publish(entity);
        return toResponse(entity);
    }

    /** Returns metadata and stats for a short code without counting a click. */
    @Transactional(readOnly = true)
    public ShortUrlResponse stats(String code) {
        return toResponse(findEntity(code));
    }

    /** Lists all short URLs, newest first. */
    @Transactional(readOnly = true)
    public PagedModel<ShortUrlResponse> list(Pageable pageable) {
        Page<ShortUrlResponse> page = repository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
        return new PagedModel<>(page);
    }

    /** Deactivates (soft-deletes) a short URL so it no longer redirects. Idempotent. */
    @Transactional
    public void deactivate(String code) {
        ShortUrl entity = findEntity(code);
        if (!entity.isActive()) {
            return; // already deactivated
        }
        entity.setActive(false);
        repository.save(entity);
        log.info("Deactivated short link '{}'", code);
    }

    /**
     * Scheduled maintenance: deactivates newly expired links and hard-deletes expired
     * links older than the configured retention window.
     */
    @Scheduled(fixedDelayString = "${app.cleanup.interval-ms:3600000}", initialDelay = 60_000)
    public void purgeExpired() {
        if (!properties.cleanup().enabled()) {
            return;
        }
        Instant now = Instant.now();
        int deactivated = repository.deactivateExpired(now);
        int deleted = repository.deleteByExpiresAtBefore(
                now.minus(Duration.ofDays(properties.cleanup().retentionDays())));
        if (deactivated > 0 || deleted > 0) {
            log.info("Cleanup: deactivated {} expired links, deleted {} beyond {}-day retention",
                    deactivated, deleted, properties.cleanup().retentionDays());
        }
    }

    private ShortUrl findEntity(String code) {
        return repository.findByShortCode(code).orElseThrow(() -> new UrlNotFoundException(code));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = Base62Codec.randomCode(properties.codeLength(), random);
            if (!RESERVED_ALIASES.contains(candidate.toLowerCase(Locale.ROOT))
                    && !repository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique short code after " + MAX_CODE_GENERATION_ATTEMPTS + " attempts");
    }

    private Instant computeExpiry(CreateShortUrlRequest request, Instant now) {
        boolean hasAbsolute = request.expiresAt() != null;
        boolean hasRelative = request.expiresInDays() != null;
        if (hasAbsolute && hasRelative) {
            throw new IllegalArgumentException("Provide either 'expiresAt' or 'expiresInDays', not both");
        }
        if (hasAbsolute) {
            Instant expiresAt = request.expiresAt().truncatedTo(ChronoUnit.SECONDS);
            if (!expiresAt.isAfter(now)) {
                throw new IllegalArgumentException("'expiresAt' must be in the future");
            }
            return expiresAt;
        }
        if (hasRelative) {
            return now.plus(Duration.ofDays(request.expiresInDays())).truncatedTo(ChronoUnit.SECONDS);
        }
        return null; // never expires
    }

    private ShortUrlResponse toResponse(ShortUrl entity) {
        return new ShortUrlResponse(
                entity.getId(),
                entity.getShortCode(),
                properties.baseUrl() + "/" + entity.getShortCode(),
                entity.getLongUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.isActive(),
                entity.getClickCount());
    }
}