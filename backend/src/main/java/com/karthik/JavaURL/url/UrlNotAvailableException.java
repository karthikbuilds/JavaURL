package com.karthik.JavaURL.url;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a short link exists but can no longer be followed because it was
 * deactivated or has expired. Maps to HTTP 410 Gone.
 */
public class UrlNotAvailableException extends RuntimeException {

    public static final HttpStatus STATUS = HttpStatus.GONE;

    public UrlNotAvailableException(String code, String reason) {
        super("Short link '" + code + "' is no longer available: " + reason);
    }
}