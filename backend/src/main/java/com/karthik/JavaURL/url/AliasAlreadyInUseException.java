package com.karthik.JavaURL.url;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested custom alias is already taken (or reserved). Maps to HTTP 409.
 */
public class AliasAlreadyInUseException extends RuntimeException {

    public static final HttpStatus STATUS = HttpStatus.CONFLICT;

    public AliasAlreadyInUseException(String alias) {
        super("Custom alias '" + alias + "' is already in use");
    }
}