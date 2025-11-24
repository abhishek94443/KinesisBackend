package com.myapp.kinesis.modules.resource.service;

import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.modules.resource.dto.ResourceAvailabilityRequest;
import com.myapp.kinesis.modules.resource.dto.ResourceAvailabilityResponse;
import com.myapp.kinesis.modules.resource.dto.ResourceRequest;
import com.myapp.kinesis.modules.resource.dto.ResourceResponse;
import com.myapp.kinesis.modules.resource.entity.ResourceAvailabilityEntity;
import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
import com.myapp.kinesis.modules.resource.repository.ResourceAvailabilityRepository;
import com.myapp.kinesis.modules.resource.repository.ResourceRepository;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.repository.VendorRepository;
import com.myapp.kinesis.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for all business logic related to Resources.
 * This service is "tenant-aware" and uses the TenantContext
 * to ensure all operations are isolated to the correct vendor.
 */
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final VendorRepository vendorRepository;
    private final TenantContext tenantContext;
    private final ResourceAvailabilityRepository availabilityRepository;

    // UPDATE CONSTRUCTOR:
    public ResourceService(ResourceRepository resourceRepository,
                           VendorRepository vendorRepository,
                           TenantContext tenantContext,
                           ResourceAvailabilityRepository availabilityRepository) {
        this.resourceRepository = resourceRepository;
        this.vendorRepository = vendorRepository;
        this.tenantContext = tenantContext;
        this.availabilityRepository = availabilityRepository;
    }

    /**
     * Creates a new resource for the *currently authenticated vendor*.
     * This logic is secure due to our "Defense in Depth" (ALT + RLS).
     */
    @Transactional
    public ResourceResponse createResource(ResourceRequest createDto) {
        // 1. ALT: Get the vendorId from the "backpack" (set by TenantInterceptor)
        UUID vendorId = tenantContext.getVendorId();

        // 2. Fetch the VendorEntity to create the association
        //    (This query is *also* secured by RLS)
        VendorEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", vendorId));

        // 3. Create the new entity
        ResourceEntity newResource = new ResourceEntity();
        newResource.setVendor(vendor);
        newResource.setName(createDto.name());
        newResource.setType(createDto.type());

        if (createDto.metadata() != null) {
            newResource.setMetadata(createDto.metadata().toString()); // Store the raw JSON string
        }

        // 4. Save to the database
        ResourceEntity savedResource = resourceRepository.save(newResource);

        // 5. Return the clean DTO
        return ResourceResponse.fromEntity(savedResource);
    }

    /**
     * Lists all resources for the *currently authenticated vendor*.
     */
    @Transactional(readOnly = true)
    public List<ResourceResponse> getResourcesForCurrentVendor() {
        // 1. ALT: Get the vendorId from the "backpack"
        UUID vendorId = tenantContext.getVendorId();

        // 2. Find all resources *only* for this vendor
        //    (This query is secured by both ALT and RLS)
        return resourceRepository.findAllByVendorId(vendorId).stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }


    //NEW FIX

    // ADD THESE FIELDS TO THE EXISTING ResourceService.java:


// ADD THESE METHODS:

    @Transactional
    public ResourceAvailabilityResponse setUnavailability(ResourceAvailabilityRequest request) {
        UUID vendorId = tenantContext.getVendorId();

        // Verify resource belongs to this vendor
        ResourceEntity resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", request.resourceId()));

        if (!resource.getVendor().getId().equals(vendorId)) {
            throw new SecurityException("Cannot set availability for resource from another vendor");
        }

        ResourceAvailabilityEntity availability = new ResourceAvailabilityEntity();
        availability.setResource(resource);
        availability.setUnavailableStart(request.unavailableStart());
        availability.setUnavailableEnd(request.unavailableEnd());
        availability.setReason(request.reason());

        ResourceAvailabilityEntity saved = availabilityRepository.save(availability);
        return ResourceAvailabilityResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ResourceAvailabilityResponse> getUnavailability(UUID resourceId) {
        UUID vendorId = tenantContext.getVendorId();

        // Verify resource belongs to this vendor
        ResourceEntity resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", resourceId));

        if (!resource.getVendor().getId().equals(vendorId)) {
            throw new SecurityException("Cannot access resource from another vendor");
        }

        return availabilityRepository.findAllByResourceId(resourceId).stream()
                .map(ResourceAvailabilityResponse::fromEntity)
                .collect(Collectors.toList());
    }
}