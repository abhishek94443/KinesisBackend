package com.myapp.kinesis.modules.resource.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.resource.dto.ResourceRequest;
import com.myapp.kinesis.modules.resource.dto.ResourceResponse;
import com.myapp.kinesis.modules.resource.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Resources (Staff, Rooms, Equipment).
 * <p>
 * All endpoints in this controller are protected by our 'staffSecurityFilterChain'
 * (because they are prefixed with /api/admin) and our TenantInterceptor.
 */
@RestController
@RequestMapping("/api/admin/resources")
// This annotation ensures only logged-in staff/owners can access
@PreAuthorize("hasAnyRole('VENDOR_OWNER', 'STAFF')")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Creates a new resource (e.g., "Dr. Smith") for the vendor.
     * The vendor is identified automatically via the JWT and TenantContext.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody ResourceRequest createDto) {

        ResourceResponse newResource = resourceService.createResource(createDto);

        return new ResponseEntity<>(
                ApiResponse.success(newResource),
                HttpStatus.CREATED
        );
    }

    /**
     * Lists all resources for the currently authenticated vendor.
     * The TenantContext ensures this service can only see its own resources.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> getResourcesForVendor() {

        List<ResourceResponse> resources = resourceService.getResourcesForCurrentVendor();
        return ResponseEntity.ok(ApiResponse.success(resources));
    }

    // We will add @PutMapping("/{id}") and @DeleteMapping("/{id}")
    // endpoints here in a future phase.
}