package com.myapp.kinesis.modules.resource.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResourceAvailabilityRequest(
        @NotNull(message = "Resource ID is required")
        UUID resourceId,

        @NotNull(message = "Start time is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime unavailableStart,

        @NotNull(message = "End time is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime unavailableEnd,

        String reason
) {
}