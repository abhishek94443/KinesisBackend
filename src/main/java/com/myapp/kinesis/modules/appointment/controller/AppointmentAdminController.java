package com.myapp.kinesis.modules.appointment.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.appointment.dto.AppointmentAdminResponse;
import com.myapp.kinesis.modules.appointment.entity.AppointmentEntity;
import com.myapp.kinesis.modules.appointment.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for VENDORS to manage their appointments.
 * <p>
 * All endpoints are secured and require a high-security (Staff) JWT.
 */
@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasAnyRole('VENDOR_OWNER', 'STAFF')")
public class AppointmentAdminController {

    private final AppointmentService appointmentService;

    public AppointmentAdminController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Lists all appointments for the currently authenticated vendor.
     * This is the main endpoint for the PWA dashboard.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentAdminResponse>>> getAppointmentsForVendor() {

        // The service gets the vendorId from the TenantContext
        List<AppointmentEntity> appointments = appointmentService.getAppointmentsForCurrentVendor();

        // Convert to the rich DTO for the admin PWA
        List<AppointmentAdminResponse> response = appointments.stream()
                .map(AppointmentAdminResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Confirms a 'PENDING' appointment.
     * (Staff/Owner action)
     */
    @PutMapping("/{appointmentId}/confirm")
    public ResponseEntity<ApiResponse<AppointmentAdminResponse>> confirmAppointment(
            @PathVariable UUID appointmentId) {

        AppointmentEntity appt = appointmentService.confirmAppointment(appointmentId);
        return ResponseEntity.ok(ApiResponse.success(AppointmentAdminResponse.fromEntity(appt)));
    }

    /**
     * Cancels an appointment.
     * (Staff/Owner action)
     */
    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<AppointmentAdminResponse>> cancelAppointment(
            @PathVariable UUID appointmentId) {

        AppointmentEntity appt = appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok(ApiResponse.success(AppointmentAdminResponse.fromEntity(appt)));
    }

    // TODO: Add endpoints for 'snooze', 'no-show', and 'manual-book'
}