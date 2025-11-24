package com.myapp.kinesis.modules.vendor.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.vendor.dto.VendorPublicDto;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.service.VendorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/vendor")
public class VendorPublicController {

    private final VendorService vendorService;

    // Inject Service, NOT Repository
    public VendorPublicController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    /**
     * Checks if a vendor slug exists.
     * Used by the Frontend Resolver to validate subdomains (e.g., clinic-a.kinesis.app).
     */
    @GetMapping("/check/{slug}")
    public ResponseEntity<ApiResponse<VendorPublicDto>> checkVendor(@PathVariable String slug) {

        // 1. Call Service (Business Logic)
        VendorEntity vendor = vendorService.getVendorBySlug(slug);

        // 2. Map to DTO (Presentation Logic)
        VendorPublicDto response = new VendorPublicDto(
                vendor.getVendorName(),
                vendor.getSlug(),
                vendor.getWebsiteConfig()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}