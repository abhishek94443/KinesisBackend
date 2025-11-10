package com.myapp.kinesis.common.exceptions;

import com.myapp.kinesis.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- NEW HANDLER FOR 403 ERRORS ---

    /**
     * Handles AuthorizationDeniedException (from @PreAuthorize) and
     * AccessDeniedException (general Spring Security).
     * This fixes the 500 error from our test.
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(Exception ex) {
        logger.warn("Access Denied: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Access Denied: You do not have the required role to perform this action."), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleTenantAccessDenied(TenantAccessDeniedException ex) {
        logger.warn("Tenant Access Denied: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse<?>> handleAuthExceptions(Exception ex) {
        // We treat "user not found" and "bad password" as the same error
        // to prevent attackers from guessing valid email addresses.
        return new ResponseEntity<>(ApiResponse.error("Invalid email or password."), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        logger.warn("Validation failed: {}", errors);
        String firstErrorMessage = errors.values().stream().findFirst().orElse("Invalid request.");
        return new ResponseEntity<>(ApiResponse.error("Validation Failed: " + firstErrorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception ex) {
        logger.error("An unexpected internal server error occurred", ex);
        return new ResponseEntity<>(ApiResponse.error("An unexpected internal server error occurred."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}