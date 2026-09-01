package com.karthik.JavaURL.analytics;

import java.time.Instant;

/**
 * Payload broadcast on {@code /topic/clicks/{shortCode}} after every redirect.
 */
public record ClickEvent(String shortCode, long totalClicks, Instant clickedAt) {
}