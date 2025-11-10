package com.myapp.kinesis.modules.resource.service;

import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.modules.resource.dto.ResourceCreateDto;
import com.myapp.kinesis.modules.resource.dto.ResourceDto;
import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
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
 */
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final VendorRepository vendorRepository;
    private final TenantContext tenantContext;

    public ResourceService(ResourceRepository resourceRepository,
                           VendorRepository vendorRepository,
                           TenantContext tenantContext) {
        this.resourceRepository = resourceRepository;
        this.vendorRepository = vendorRepository;
        this.tenantContext = tenantContext;
    }

    /**
     * Creates a new resource for the *currently authenticated vendor*.
     */
    @Transactional
    public ResourceDto createResource(ResourceCreateDto createDto) {
        // 1. Get the vendorId from the "backpack" (set by TenantInterceptor)
        UUID vendorId = tenantContext.getVendorId();

        // 2. Fetch the VendorEntity to create the association
        VendorEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", vendorId));

        // 3. Create the new entity
        ResourceEntity newResource = new ResourceEntity();
        newResource.setVendor(vendor);
        newResource.setName(createDto.name());
        newResource.setType(createDto.type());
        newResource.setMetadata(createDto.metadata()); // Store the raw JSON string

        // 4. Save to the database
        ResourceEntity savedResource = resourceRepository.save(newResource);

        // 5. Return the clean DTO
        return ResourceDto.fromEntity(savedResource);
    }

    /**
     * Lists all resources for the *currently authenticated vendor*.
     */
    @Transactional(readOnly = true)
    public List<ResourceDto> getResourcesForCurrentVendor() {
        // 1. Get the vendorId from the "backpack"
        UUID vendorId = tenantContext.getVendorId();

        // 2. Find all resources *only* for this vendor
        return resourceRepository.findAllByVendorId(vendorId).stream()
                .map(ResourceDto::fromEntity)
                .collect(Collectors.toList());
    }
}