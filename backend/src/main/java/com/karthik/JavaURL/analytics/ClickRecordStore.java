package com.karthik.JavaURL.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Persists per-click analytics on a background worker so the redirect stays fast.
 * Failures are logged and swallowed — analytics must never break a redirect.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClickRecordStore {

    private final ClickRecordRepository repository;

    @Async("clickRecordExecutor")
    public void record(String shortCode, String referer, String userAgent, String ip) {
        try {
            repository.save(new ClickRecord(shortCode, Instant.now(), truncate(referer, 2048),
                    truncate(userAgent, 512), truncate(ip, 64)));
        } catch (Exception ex) {
            log.warn("Could not record click for '{}': {}", shortCode, ex.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}