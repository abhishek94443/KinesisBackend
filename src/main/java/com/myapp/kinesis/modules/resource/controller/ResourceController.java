package com.myapp.kinesis.modules.resource.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.resource.dto.ResourceCreateDto;
import com.myapp.kinesis.modules.resource.dto.ResourceDto;
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
 * All endpoints in this controller are protected and require a
 * high-security (Staff) JWT and are prefixed with /api/admin.
 */
@RestController
@RequestMapping("/api/admin/resources")
// This annotation ensures only logged-in staff/owners can access these endpoints
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
    public ResponseEntity<ApiResponse<ResourceDto>> createResource(
            @Valid @RequestBody ResourceCreateDto createDto) {

        ResourceDto newResource = resourceService.createResource(createDto);
        return new ResponseEntity<>(
                ApiResponse.success(newResource),
                HttpStatus.CREATED
        );
    }

    /**
     * Lists all resources for the currently authenticated vendor.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceDto>>> getResourcesForVendor() {

        List<ResourceDto> resources = resourceService.getResourcesForCurrentVendor();
        return ResponseEntity.ok(ApiResponse.success(resources));
    }

    // TODO: We will add @PutMapping("/{id}") and @DeleteMapping("/{id}")
    // endpoints here in a future phase.
}