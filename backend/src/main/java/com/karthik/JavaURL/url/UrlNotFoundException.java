package com.karthik.JavaURL.url;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a short code does not exist. Maps to HTTP 404.
 */
public class UrlNotFoundException extends RuntimeException {

    public static final HttpStatus STATUS = HttpStatus.NOT_FOUND;

    public UrlNotFoundException(String code) {
        super("No short URL found for code '" + code + "'");
    }
}