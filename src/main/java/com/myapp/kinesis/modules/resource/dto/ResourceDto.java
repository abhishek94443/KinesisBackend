package com.myapp.kinesis.modules.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.common.enums.ResourceType;
import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
import lombok.SneakyThrows;

import java.util.UUID;

/**
 * DTO (Data Transfer Object) for securely sending Resource data to the frontend.
 * This record includes a helper method to safely convert the 'metadata'
 * string into a real JSON object for the frontend.
 */
public record ResourceDto(
        UUID id,
        UUID vendorId,
        String name,
        ResourceType type,
        JsonNode metadata // We send clean JSON, not a string
) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * A "static factory method" to safely convert a ResourceEntity (from the DB)
     * into a ResourceDto (for the API). This is a professional pattern.
     *
     * @param entity The ResourceEntity from the database.
     * @return A new ResourceDto.
     */
    @SneakyThrows // Lombok annotation to handle the "try/catch" for JSON parsing
    public static ResourceDto fromEntity(ResourceEntity entity) {

        JsonNode metadataNode = null;
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            // Parse the raw metadata string into a clean JSON object
            metadataNode = objectMapper.readTree(entity.getMetadata());
        }

        return new ResourceDto(
                entity.getId(),
                entity.getVendor().getId(),
                entity.getName(),
                entity.getType(),
                metadataNode
        );
    }
}