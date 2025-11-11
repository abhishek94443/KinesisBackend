package com.myapp.kinesis.common.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Custom exception (Fix 1B)
 * Thrown by JwtService when a token is malformed, has an invalid
 * signature, or fails any other validation.
 */
public class JwtTokenValidationException extends AuthenticationException {
    public JwtTokenValidationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}