package com.myapp.kinesis.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * A standard, consistent JSON response wrapper for all API endpoints.
 * Using a standard wrapper is a best practice for API design.
 * * We use Java 21's 'record' for a concise, immutable data carrier.
 * * @JsonInclude(JsonInclude.Include.NON_NULL) prevents 'null' fields
 * from being included in the final JSON response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data, // This will hold our main payload (e.g., a Client)
        OffsetDateTime timestamp
) {

    /**
     * Helper factory method for a simple success response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Helper factory method for a simple success response with no data.
     */
    public static ApiResponse<?> success(String message) {
        return new ApiResponse<>(true, message, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Helper factory method for a simple error response.
     */
    public static ApiResponse<?> error(String message) {
        return new ApiResponse<>(false, message, null, OffsetDateTime.now(ZoneOffset.UTC));
    }
}