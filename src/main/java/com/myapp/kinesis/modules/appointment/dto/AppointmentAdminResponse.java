package com.myapp.kinesis.modules.appointment.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.modules.appointment.entity.AppointmentEntity;
import com.myapp.kinesis.modules.customer.dto.ClientResponse;
import com.myapp.kinesis.modules.service.dto.ServiceResponseDto;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * v2 (Corrected):
 * This DTO now correctly references the v2 ClientResponse DTO.
 */
public record AppointmentAdminResponse(
        UUID appointmentId,
        String status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        OffsetDateTime createdAt,
        BigDecimal priceCharged,
        JsonNode metadata, // The custom *answers* from the form

        // Contact info for the BOOKER (for family bookings)
        String bookingContactName,
        String bookingContactEmail,
        String bookingContactPhone,

        // Linked objects, converted to their own DTOs
        ClientResponse client,
        ServiceResponseDto service
) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Static factory method to convert the DB entity into our clean DTO.
     * This is a "deep" conversion.
     */
    @SneakyThrows
    public static AppointmentAdminResponse fromEntity(AppointmentEntity entity) {

        JsonNode metadataNode = null;
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            metadataNode = objectMapper.readTree(entity.getMetadata());
        }

        // This fromEntity() call now matches our corrected v2 ClientResponse
        ClientResponse clientDto = ClientResponse.fromEntity(entity.getClient());

        // This assumes ServiceResponseDto.fromEntity(entity.getService()) exists.
        // We will simplify this for now.
        // TODO: We must build a full ServiceResponseDto.fromEntity() later.
        ServiceResponseDto serviceDto = ServiceResponseDto.fromEntity(entity.getService());

        return new AppointmentAdminResponse(
                entity.getId(),
                entity.getStatus().name(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCreatedAt(),
                entity.getPriceCharged(),
                metadataNode,
                entity.getBookingContactName(),
                entity.getBookingContactEmail(),
                entity.getBookingContactPhone(),
                clientDto,
                serviceDto
        );
    }
}