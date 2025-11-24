package com.myapp.kinesis.modules.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.myapp.kinesis.modules.appointment.entity.AppointmentEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTOs for the /api/public/book endpoint.
 */
public class BookingDto {

    /**
     * This is the REQUEST body for a "Guest" booking.
     */
    public record BookingRequest(
            @NotNull(message = "Service ID is required")
            UUID serviceId,

            @NotNull(message = "Start time is required")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime startTime,

            // --- Client (Guest) Information ---
            @NotBlank(message = "First name is required")
            String firstName,
            String lastName,

            @NotBlank(message = "Email is required")
            @Email
            String email,

            @NotBlank(message = "Phone is required")
            String phone,

            /**
             * The JSON object of *answers* to the custom form.
             * e.g., { "reason_for_visit": "Annual checkup" }
             */
            JsonNode metadata
    ) {
    }

    /**
     * This is the RESPONSE body we send back after a successful booking.
     */
    public record BookingResponse(
            UUID appointmentId,
            UUID vendorId,
            UUID clientId,
            UUID serviceId,
            String serviceName,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime startTime,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime endTime,

            String status,
            String contactName
    ) {
        /**
         * Static factory method to convert an AppointmentEntity
         * into a clean response DTO.
         */
        public static BookingResponse fromEntity(AppointmentEntity entity) {
            return new BookingResponse(
                    entity.getId(),
                    entity.getVendor().getId(),
                    entity.getClient().getId(),
                    entity.getService().getId(),
                    entity.getService().getName(),
                    entity.getStartTime(),
                    entity.getEndTime(),
                    entity.getStatus().name(),
                    entity.getBookingContactName()
            );
        }
    }
}