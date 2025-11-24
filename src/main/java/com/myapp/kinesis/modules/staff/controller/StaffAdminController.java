package com.myapp.kinesis.modules.staff.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for VENDOR_OWNERs and STAFF to manage their own silo.
 * All endpoints are secure and require a 'Staff' JWT.
 */
@RestController
@RequestMapping("/api/admin") // All routes here are prefixed with /api/admin
public class StaffAdminController {

    private final TenantContext tenantContext;

    // We inject the TenantContext "backpack"
    public StaffAdminController(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * This is our "Test Endpoint".
     * It is secured by @PreAuthorize, so only a user with a valid
     * Staff/Owner role can even access it.
     *
     * It reads the vendorId from the TenantContext "backpack" which
     * our TenantInterceptor has already set.
     */
    @GetMapping("/context-test")
    @PreAuthorize("hasAnyRole('VENDOR_OWNER', 'STAFF')") // Requires a VENDOR_OWNER or STAFF role
    public ResponseEntity<ApiResponse<Map<String, UUID>>> getTenantContext() {

        // This is the core of the test.
        // We read the vendorId that the TenantInterceptor put in the "backpack".
        UUID currentVendorId = tenantContext.getVendorId();

        Map<String, UUID> responseData = Map.of("currentVendorId", currentVendorId);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    // TODO: We will add endpoints for inviting staff, etc. here.
}