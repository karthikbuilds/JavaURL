package com.karthik.JavaURL.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Typed application configuration bound from the {@code app.*} properties.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, int codeLength, int redirectStatus, Cleanup cleanup, Broker broker) {

    private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 307, 308);
    private static final int DEFAULT_CODE_LENGTH = 7;

    public AppProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8081";
        }
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (codeLength < 3 || codeLength > 32) {
            codeLength = DEFAULT_CODE_LENGTH;
        }
        if (!REDIRECT_STATUSES.contains(redirectStatus)) {
            redirectStatus = 302;
        }
        cleanup = cleanup == null ? new Cleanup(true, 30) : cleanup;
        broker = broker == null ? new Broker("simple", "localhost", 61613) : broker;
    }

    public record Cleanup(boolean enabled, int retentionDays) {
        public Cleanup {
            if (retentionDays <= 0) {
                retentionDays = 30;
            }
        }
    }

    /**
     * WebSocket message broker settings. Defaults to the in-process {@code simple}
     * broker; set {@code type=external} with your STOMP broker host/port when the
     * backend is scaled across multiple instances so broadcasts stay shared.
     */
    public record Broker(String type, String host, int port) {
        public Broker {
            if (type == null || type.isBlank()) type = "simple";
            if (host == null || host.isBlank()) host = "localhost";
            if (port <= 0) port = 61613;
        }
    }
}