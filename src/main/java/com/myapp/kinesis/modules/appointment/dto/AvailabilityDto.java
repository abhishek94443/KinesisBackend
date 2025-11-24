package com.myapp.kinesis.modules.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for the /api/public/availability endpoint.
 * v2 (Corrected): Changed @NotBlank to @NotNull for serviceId (UUID).
 */
public class AvailabilityDto {

    /**
     * This is the REQUEST body the public website will send.
     */
    public record AvailabilityRequest(
            @NotNull(message = "Service ID is required") // <-- THE FIX
            UUID serviceId,

            @NotNull(message = "Date is required")
            @FutureOrPresent(message = "Cannot check availability for past dates")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            LocalDate date
    ) {
    }

    /**
     * This is the RESPONSE body the backend will send.
     */
    public record AvailabilityResponse(
            List<Slot> availableSlots
    ) {
    }

    /**
     * Represents a single available time slot.
     */
    public record Slot(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime startTime,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime endTime
    ) {
    }
}