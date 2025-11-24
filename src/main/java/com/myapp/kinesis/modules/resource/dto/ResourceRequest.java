package com.myapp.kinesis.modules.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.myapp.kinesis.common.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO (Data Transfer Object) for creating or updating a Resource.
 * This is the JSON body the PWA will send.
 * We use a Java 21 record.
 */
public record ResourceRequest(

        @NotBlank(message = "Resource name is required (e.g., 'Dr. Smith', 'Room 1')")
        String name,

        @NotNull(message = "Resource type is required")
        ResourceType type, // This will be 'STAFF', 'ROOM', or 'EQUIPMENT'

        /**
         * Optional. A JSON object containing custom metadata.
         * e.g., { "title": "Senior Dentist", "bio": "..." }
         * We accept it as a JsonNode to ensure it's valid JSON.
         */
        JsonNode metadata
) {
}