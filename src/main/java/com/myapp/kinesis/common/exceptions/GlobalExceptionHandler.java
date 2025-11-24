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
 * v4 (Corrected):
 * - Added handler for IllegalStateException (to fix 500 on double booking).
 * - Added handler for SecurityException (to fix 500 on tenant isolation).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- NEW HANDLER FOR BUG 3 (Tenant Isolation) ---
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<?>> handleSecurityException(SecurityException ex) {
        // This catches our "Cannot link resource from another vendor."
        logger.warn("Security Exception (Tenant Isolation Failure): {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    // --- NEW HANDLER FOR BUG 2 (Business Logic) ---
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException ex) {
        // This catches our "Slot no longer available" error.
        logger.warn("Business Logic Exception: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // --- Existing JWT/Auth Handlers ---
    @ExceptionHandler(JwtTokenExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtExpired(JwtTokenExpiredException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtTokenValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtValidation(JwtTokenValidationException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConflict(DataIntegrityViolationException ex) {
        logger.warn("Data integrity violation: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Conflict: This resource (e.g., email or slug) already exists."), HttpStatus.CONFLICT);
    }

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
        // Catches "Email already in use"
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()
                ));

        logger.warn("Validation failed: {}", errors);
        ApiResponse<?> errorResponse = new ApiResponse<>(false, "Validation Failed", errors, null);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception ex) {
        logger.error("An unexpected internal server error occurred", ex);
        return new ResponseEntity<>(ApiResponse.error("An unexpected internal server error occurred."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}