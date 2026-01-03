package com.myapp.kinesis.modules.vendor.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.vendor.dto.VendorWebsiteConfigRequest;
import com.myapp.kinesis.modules.vendor.dto.VendorWebsiteConfigResponse;
import com.myapp.kinesis.modules.vendor.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Vendor Admin to manage their website configuration.
 */
@RestController
@RequestMapping("/api/admin/vendor")
@PreAuthorize("hasAnyRole('VENDOR_OWNER', 'STAFF')")
public class VendorAdminController {

    private final VendorService vendorService;

    public VendorAdminController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    /**
     * Get website configuration for the vendor's slug.
     * The slug must match the currently authenticated vendor.
     * 
     * GET /api/admin/vendor/sunshine-dental/website-config
     */
    @GetMapping("/{slug}/website-config")
    public ResponseEntity<ApiResponse<VendorWebsiteConfigResponse>> getWebsiteConfig(
            @PathVariable String slug) {
        
        VendorWebsiteConfigResponse config = vendorService.getWebsiteConfig(slug);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * Update website configuration for the vendor's slug.
     * The slug must match the currently authenticated vendor.
     * 
     * PUT /api/admin/vendor/sunshine-dental/website-config
     */
    @PutMapping("/{slug}/website-config")
    public ResponseEntity<ApiResponse<VendorWebsiteConfigResponse>> updateWebsiteConfig(
            @PathVariable String slug,
            @Valid @RequestBody VendorWebsiteConfigRequest request) {
        
        VendorWebsiteConfigResponse config = vendorService.updateWebsiteConfig(
            slug, 
            request.websiteConfig()
        );
        
        return ResponseEntity.ok(
            ApiResponse.success(config)
        );
    }
}