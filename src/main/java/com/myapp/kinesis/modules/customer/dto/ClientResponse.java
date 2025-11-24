package com.myapp.kinesis.modules.customer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import lombok.SneakyThrows;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * v2 (Corrected): This DTO now correctly maps to our "Air-Gap" ClientEntity.
 * The 'clientId' *is* the 'userId' for the customer portal.
 */
public record ClientResponse(
        UUID clientId, // This is the unique ID for this customer profile
        UUID vendorId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String status, // e.g., "ACTIVE", "BLOCKED_BY_VENDOR"
        JsonNode metadata, // e.g., { "dob": "...", "insurance_id": "..." }
        OffsetDateTime createdAt
) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * This is the corrected, v2 factory method.
     * It no longer looks for a '.getUser()' method.
     * The ClientEntity's ID ('.getId()') is the 'userId' for this context.
     */
    @SneakyThrows
    public static ClientResponse fromEntity(ClientEntity entity) {

        JsonNode metadataNode = null;
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            metadataNode = objectMapper.readTree(entity.getMetadata());
        }

        return new ClientResponse(
                entity.getId(), // <-- The Fix: The ID comes directly from the entity
                entity.getVendor().getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getStatus(),
                metadataNode,
                entity.getCreatedAt()
        );
    }
}