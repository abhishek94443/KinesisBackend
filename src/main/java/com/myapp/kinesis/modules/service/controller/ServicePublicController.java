package com.myapp.kinesis.modules.service.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.service.dto.ServiceResponseDto;
import com.myapp.kinesis.modules.service.service.ServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for PUBLIC access to a vendor's service catalog.
 *
 * All endpoints are public (permitAll) and are prefixed with /api/public.
 */
@RestController
@RequestMapping("/api/public/services")
public class ServicePublicController {

    private final ServiceService serviceService;

    public ServicePublicController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    /**
     * Lists all *active* services for a specific vendor, identified
     * by their public slug.
     */
    @GetMapping("/{vendorSlug}")
    public ResponseEntity<ApiResponse<List<ServiceResponseDto>>> getPublicServicesForVendor(
            @PathVariable String vendorSlug) {

        List<ServiceResponseDto> services = serviceService.getPublicServicesForVendor(vendorSlug);
        return ResponseEntity.ok(ApiResponse.success(services));
    }
}