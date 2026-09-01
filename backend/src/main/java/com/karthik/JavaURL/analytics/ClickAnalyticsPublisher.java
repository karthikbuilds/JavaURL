package com.karthik.JavaURL.analytics;

import com.karthik.JavaURL.config.WebSocketConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Pushes live click statistics to WebSocket subscribers of a given short link.
 * Failures are logged and swallowed: analytics must never break a redirect.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClickAnalyticsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(String shortCode, long totalClicks) {
        try {
            messagingTemplate.convertAndSend(
                    WebSocketConfig.TOPIC_PREFIX + shortCode,
                    new ClickEvent(shortCode, totalClicks, Instant.now()));
        } catch (Exception ex) {
            log.warn("Could not publish click event for '{}': {}", shortCode, ex.getMessage());
        }
    }
}