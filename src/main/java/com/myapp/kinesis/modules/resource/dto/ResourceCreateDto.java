package com.myapp.kinesis.modules.resource.dto;

import com.myapp.kinesis.common.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO (Data Transfer Object) for creating a new Resource.
 * This is the JSON body the PWA will send.
 */
public record ResourceCreateDto(

        @NotBlank(message = "Resource name is required (e.g., 'Dr. Smith', 'Room 1')")
        String name,

        @NotNull(message = "Resource type is required")
        ResourceType type, // This will be 'STAFF', 'ROOM', or 'EQUIPMENT'

        /**
         * Optional. A JSON string containing custom metadata.
         * e.g., { "title": "Senior Dentist", "bio": "..." }
         */
        String metadata
) {
}