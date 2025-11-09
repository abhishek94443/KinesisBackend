package com.myapp.kinesis.common.exceptions;

import com.myapp.kinesis.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler (@RestControllerAdvice).
 * This class acts as a "master safety net" for all @RestControllers.
 * It intercepts exceptions and formats them into a clean ApiResponse JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles our custom "Wrong Silo" (401) exception.
     */
    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleTenantAccessDenied(TenantAccessDeniedException ex) {
        logger.warn("Tenant Access Denied: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles Spring Security's "Wrong Password" exception.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException ex) {
        return new ResponseEntity<>(ApiResponse.error("Invalid email or password."), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles our custom "Not Found" (404) exception.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles DTO @Valid annotation failures (400).
     * This overrides the default Spring implementation to return our custom ApiResponse.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        logger.warn("Validation failed: {}", errors);
        return new ResponseEntity<>(ApiResponse.error("Validation Failed: " + errors), HttpStatus.BAD_REQUEST);
    }

    /**
     * A general-purpose "catch-all" for any other 500-level server error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception ex) {
        // We log the full stack trace for 500 errors, as they are unexpected.
        logger.error("An unexpected internal server error occurred", ex);
        return new ResponseEntity<>(ApiResponse.error("An unexpected internal server error occurred."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}