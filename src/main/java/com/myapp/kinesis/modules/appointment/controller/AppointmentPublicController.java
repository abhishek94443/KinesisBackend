package com.myapp.kinesis.modules.appointment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.appointment.dto.AvailabilityDto;
import com.myapp.kinesis.modules.appointment.dto.BookingDto;
import com.myapp.kinesis.modules.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for PUBLIC access to a vendor's booking system.
 * <p>
 * All endpoints are public (permitAll) and are prefixed with /api/public.
 */
@RestController
@RequestMapping("/api/public")
public class AppointmentPublicController {

    private final AppointmentService appointmentService;

    public AppointmentPublicController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Gets all available slots for a given service and date.
     * This is the "brain" of the booking calendar.
     */
    @PostMapping("/availability/{vendorSlug}")
    public ResponseEntity<ApiResponse<AvailabilityDto.AvailabilityResponse>> getAvailability(
            @PathVariable String vendorSlug,
            @Valid @RequestBody AvailabilityDto.AvailabilityRequest request) throws JsonProcessingException {

        AvailabilityDto.AvailabilityResponse response =
                appointmentService.getAvailability(vendorSlug, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Creates a new "Guest" booking.
     */
    @PostMapping("/book/{vendorSlug}")
    public ResponseEntity<ApiResponse<BookingDto.BookingResponse>> createGuestBooking(
            @PathVariable String vendorSlug,
            @Valid @RequestBody BookingDto.BookingRequest request) throws JsonProcessingException {

        BookingDto.BookingResponse response =
                appointmentService.createGuestBooking(vendorSlug, request);

        return new ResponseEntity<>(
                ApiResponse.success(response),
                HttpStatus.CREATED
        );
    }
}