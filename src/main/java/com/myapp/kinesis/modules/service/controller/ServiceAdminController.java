package com.myapp.kinesis.modules.service.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.service.dto.ServiceRequestDto;
import com.myapp.kinesis.modules.service.dto.ServiceResponseDto;
import com.myapp.kinesis.modules.service.service.ServiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for VENDORS to manage their Service Catalog.
 * <p>
 * All endpoints are secured and require a high-security (Staff) JWT.
 */
@RestController
@RequestMapping("/api/admin/services")
@PreAuthorize("hasAnyRole('VENDOR_OWNER', 'STAFF')")
public class ServiceAdminController {

    private final ServiceService serviceService;

    public ServiceAdminController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    /**
     * Creates a new service (e.g., "Yoga Class") for the vendor.
     * The vendor is identified automatically via the JWT and TenantContext.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceResponseDto>> createService(
            @Valid @RequestBody ServiceRequestDto createDto) {

        ServiceResponseDto newService = serviceService.createService(createDto);
        return new ResponseEntity<>(
                ApiResponse.success(newService),
                HttpStatus.CREATED
        );
    }

    /**
     * Lists all services (active and inactive) for the
     * currently authenticated vendor's PWA dashboard.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponseDto>>> getServicesForVendor() {

        List<ServiceResponseDto> services = serviceService.getServicesForCurrentVendor();
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    // TODO: We will add @PutMapping("/{serviceId}") and @DeleteMapping("/{serviceId}")
}