package com.karthik.JavaURL.url.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error payload returned by all endpoints.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        String path,
        Map<String, String> fieldErrors
) {

    public ErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, Instant.now(), path, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, Instant.now(), path, fieldErrors);
    }
}