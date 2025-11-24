package com.myapp.kinesis.modules.resource.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.myapp.kinesis.modules.resource.entity.ResourceAvailabilityEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResourceAvailabilityResponse(
        UUID id,
        UUID resourceId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime unavailableStart,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime unavailableEnd,
        String reason
) {
    public static ResourceAvailabilityResponse fromEntity(ResourceAvailabilityEntity entity) {
        return new ResourceAvailabilityResponse(
                entity.getId(),
                entity.getResource().getId(),
                entity.getUnavailableStart(),
                entity.getUnavailableEnd(),
                entity.getReason()
        );
    }
}