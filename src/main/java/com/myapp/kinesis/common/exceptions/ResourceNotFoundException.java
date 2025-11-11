package com.myapp.kinesis.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A custom, semantic exception that represents a 404 Not Found error.
 * * @ResponseStatus(HttpStatus.NOT_FOUND) tells Spring to automatically
 * return a 404 status code whenever this exception is thrown
 * and not caught by our GlobalExceptionHandler.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}