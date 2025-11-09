package com.myapp.kinesis.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A custom, semantic exception for a 401 Unauthorized error.
 * <p>
 * This is thrown when a user is *authenticated* (their password is correct)
 * but they are *not authorized* to access a specific vendor's silo.
 * This is the core of our "Contextual Login" security.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}