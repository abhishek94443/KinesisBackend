package com.myapp.kinesis.modules.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.modules.resource.dto.ResourceResponse;
import com.myapp.kinesis.modules.service.entity.ServiceEntity;
import com.myapp.kinesis.modules.service.entity.ServiceResourceEntity;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO (Data Transfer Object) for securely sending Service data to the frontend.
 * This includes the list of linked resources.
 */
public record ServiceResponseDto(
        UUID id,
        UUID vendorId,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price,
        boolean isActive,
        JsonNode metadata,
        List<ResourceResponse> resources ,// A list of all linked resources
        // NEW FIELDS
        String imageUrl,
        String category,
        Integer displayOrder,
        String currency
) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Static factory method to convert a ServiceEntity (from the DB)
     * into a ServiceResponseDto (for the API).
     */
    @SneakyThrows
    public static ServiceResponseDto fromEntity(ServiceEntity entity, List<ServiceResourceEntity> resourceLinks) {

        JsonNode metadataNode = null;
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            metadataNode = objectMapper.readTree(entity.getMetadata());
        }

        // Convert the linked entities into DTOs
        List<ResourceResponse> resourceDtos = resourceLinks.stream()
                .map(link -> ResourceResponse.fromEntity(link.getResource()))
                .collect(Collectors.toList());

        return new ServiceResponseDto(
                entity.getId(),
                entity.getVendor().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getPrice(),
                entity.isActive(),
                metadataNode,
                resourceDtos,
                entity.getImageUrl(),       // NEW
                entity.getCategory(),       // NEW
                entity.getDisplayOrder(),   // NEW
                entity.getCurrency()        // NEW
        );
    }

    /**
     * Overloaded factory method for when we don't have the links
     */
    @SneakyThrows
    public static ServiceResponseDto fromEntity(ServiceEntity entity) {
        JsonNode metadataNode = null;
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            metadataNode = objectMapper.readTree(entity.getMetadata());
        }

        return new ServiceResponseDto(
                entity.getId(),
                entity.getVendor().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getPrice(),
                entity.isActive(),
                metadataNode,
                List.of(), // Empty list
                entity.getImageUrl(),       // NEW
                entity.getCategory(),       // NEW
                entity.getDisplayOrder(),   // NEW
                entity.getCurrency()        // NEW
        );
    }
}