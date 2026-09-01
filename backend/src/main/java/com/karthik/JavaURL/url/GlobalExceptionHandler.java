package com.karthik.JavaURL.url;

import com.karthik.JavaURL.url.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates exceptions into consistent JSON error responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UrlNotFoundException ex, HttpServletRequest request) {
        return build(UrlNotFoundException.STATUS, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AliasAlreadyInUseException.class)
    public ResponseEntity<ErrorResponse> handleAliasConflict(AliasAlreadyInUseException ex, HttpServletRequest request) {
        return build(AliasAlreadyInUseException.STATUS, ex.getMessage(), request, null);
    }

    @ExceptionHandler(UrlNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleGone(UrlNotAvailableException ex, HttpServletRequest request) {
        return build(UrlNotAvailableException.STATUS, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                Map<String, String> fieldErrors) {
        ErrorResponse body = ErrorResponse.of(status, message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}