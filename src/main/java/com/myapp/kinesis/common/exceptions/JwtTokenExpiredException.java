package com.myapp.kinesis.common.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Custom exception (Fix 1B)
 * Thrown by JwtService when a token is expired.
 * This will be caught by the GlobalExceptionHandler.
 */
public class JwtTokenExpiredException extends AuthenticationException {
    public JwtTokenExpiredException(String msg, Throwable cause) {
        super(msg, cause);
    }
}