package com.karthik.JavaURL.url;

import com.karthik.JavaURL.analytics.ClickAnalyticsPublisher;
import com.karthik.JavaURL.analytics.ClickDetailResponse;
import com.karthik.JavaURL.analytics.ClickRecord;
import com.karthik.JavaURL.analytics.ClickRecordRepository;
import com.karthik.JavaURL.config.AppProperties;
import com.karthik.JavaURL.util.Base62Codec;
import com.karthik.JavaURL.url.dto.CreateShortUrlRequest;
import com.karthik.JavaURL.url.dto.ShortUrlResponse;
import lombok.extern.slf4j.Slf4j;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
@Slf4j
public class UrlShortenerService {

    /** Aliases that would shadow built-in routes or infrastructure endpoints. */
    static final Set<String> RESERVED_ALIASES = Set.of("api", "ws", "actuator", "error");

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final ShortUrlRepository repository;
    private final AppProperties properties;
    private final ClickAnalyticsPublisher analyticsPublisher;
    private final ClickRecordRepository clickRecordRepository;
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

    public UrlShortenerService(ShortUrlRepository repository, AppProperties properties,
                               ClickAnalyticsPublisher analyticsPublisher,
                               ClickRecordRepository clickRecordRepository) {
        this.repository = repository;
        this.properties = properties;
        this.analyticsPublisher = analyticsPublisher;
        this.clickRecordRepository = clickRecordRepository;
    }

    /** In-process cache of entity metadata (destination, expiry, active flag) for hot redirects. */
    private final Cache<String, ShortUrl> redirectCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats()
            .build();

    /**
     * Resolves a short code for redirection: validates availability/expiry,
     * atomically increments the click counter and broadcasts the new count over WebSocket.
     * The increment runs as a single SQL statement so concurrent redirects never lose an update.
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

        int updated = repository.incrementClickCount(code); // atomic DB-side increment
        if (updated == 0) {
            // Lost a race with a concurrent deactivation; reload the truth.
            evictFromCache(code);
            throw new UrlNotAvailableException(code, "it was deactivated");
        }
        long newCount = entity.getClickCount() + 1;
        entity.setClickCount(newCount);
        analyticsPublisher.publish(code, newCount);
        return toResponse(entity);
    }

    /** Returns metadata and stats for a short code without counting a click. */
    @Transactional(readOnly = true)
    public ShortUrlResponse stats(String code) {
        return toResponse(findEntity(code));
    }

    /** Returns the most recent click details for a short code (for the analytics view). */
    @Transactional(readOnly = true)
    public java.util.List<ClickDetailResponse> recentClicks(String code, int limit) {
        findEntity(code); // ensure the code exists (404 if it does not)
        return clickRecordRepository.findTop50ByShortCodeOrderByClickedAtDesc(code)
                .stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(this::toClickDetail)
                .toList();
    }

    private ClickDetailResponse toClickDetail(ClickRecord record) {
        return new ClickDetailResponse(
                record.getShortCode(), record.getClickedAt(),
                record.getReferer(), record.getUserAgent(), record.getIp());
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
        evictFromCache(code);
        log.info("Deactivated short link '{}'", code);
    }

    /**
     * Scheduled maintenance: deactivates newly expired links and hard-deletes expired
     * links older than the configured retention window.
     */
    @Transactional
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
            redirectCache.invalidateAll(); // drop any now-stale cached entries
            log.info("Cleanup: deactivated {} expired links, deleted {} beyond {}-day retention",
                    deactivated, deleted, properties.cleanup().retentionDays());
        }
    }

    private ShortUrl findEntity(String code) {
        ShortUrl cached = redirectCache.getIfPresent(code);
        if (cached != null) {
            return cached;
        }
        ShortUrl entity = repository.findByShortCode(code).orElseThrow(() -> new UrlNotFoundException(code));
        redirectCache.put(code, entity);
        return entity;
    }

    private void evictFromCache(String code) {
        redirectCache.invalidate(code);
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