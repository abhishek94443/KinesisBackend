package com.myapp.kinesis.modules.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) for creating or updating a Service.
 * This is the JSON body the PWA will send.
 */
public record ServiceRequestDto(

        @NotBlank(message = "Service name is required")
        String name,

        String description,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        int durationMinutes,

        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price cannot be negative")
        BigDecimal price,

        @NotNull(message = "is_active flag is required")
        boolean isActive,

        
     // NEW FIELDS
        String imageUrl,
        String category,
        Integer displayOrder,
        String currency,
        /**
         * The business rules for this service.
         * e.g., { "booking_model": "SCHEDULED", "capacity": 25 }
         */
        JsonNode metadata,

        /**
         * A list of Resource UUIDs that are required for this service.
         * e.g., ["uuid-for-dr-smith", "uuid-for-room-1"]
         */
        @NotNull
        List<UUID> resourceIds
) {
}