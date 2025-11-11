package com.myapp.kinesis.common.exceptions;

import com.myapp.kinesis.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

import java.util.Map;
import java.util.stream.Collectors;

/**
 * v4 (Hardened): Implements reviewer feedback.
 * - (Fix 4) Catches the new JwtTokenExpiredException and JwtTokenValidationException.
 * - (Fix 4) Catches DataIntegrityViolationException for 409 CONFLICT.
 * - (Fix 4) Returns a full map of validation errors instead of just the first one.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- NEW HANDLERS FOR JWT FAILURES (Fix 4) ---
    @ExceptionHandler(JwtTokenExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtExpired(JwtTokenExpiredException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtTokenValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtValidation(JwtTokenValidationException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    // --- NEW HANDLER FOR DATABASE CONFLICTS (Fix 4) ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConflict(DataIntegrityViolationException ex) {
        // This catches our UNIQUE constraint violations (e.g., duplicate email or slug)
        logger.warn("Data integrity violation: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Conflict: This resource (e.g., email or slug) already exists."), HttpStatus.CONFLICT);
    }

    // --- EXISTING HANDLERS ---
    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleTenantAccessDenied(TenantAccessDeniedException ex) {
        logger.warn("Tenant Access Denied: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse<?>> handleAuthExceptions(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error("Invalid email or password."), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(Exception ex) {
        logger.warn("Access Denied: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Access Denied: You do not have the required role to perform this action."), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    /**
     * (Fix 4) Upgraded to return a full map of validation errors.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        // This is a cleaner way to get all errors
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()
                ));

        logger.warn("Validation failed: {}", errors);

        // We create a new ApiResponse where the 'data' field *is* the map of errors
        ApiResponse<?> errorResponse = new ApiResponse<>(false, "Validation Failed", errors, null);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception ex) {
        logger.error("An unexpected internal server error occurred", ex);
        return new ResponseEntity<>(ApiResponse.error("An unexpected internal server error occurred."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}