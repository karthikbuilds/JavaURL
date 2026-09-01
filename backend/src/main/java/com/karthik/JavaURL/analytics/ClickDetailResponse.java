package com.karthik.JavaURL.analytics;

import java.time.Instant;

/**
 * Serialisable summary of a single recorded click.
 */
public record ClickDetailResponse(String shortCode, Instant clickedAt, String referer, String userAgent, String ip) {
}