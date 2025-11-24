package com.myapp.kinesis.modules.service.service;

import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
import com.myapp.kinesis.modules.resource.repository.ResourceRepository;
import com.myapp.kinesis.modules.service.dto.ServiceRequestDto;
import com.myapp.kinesis.modules.service.dto.ServiceResponseDto;
import com.myapp.kinesis.modules.service.entity.ServiceEntity;
import com.myapp.kinesis.modules.service.entity.ServiceResourceEntity;
import com.myapp.kinesis.modules.service.repository.ServiceRepository;
import com.myapp.kinesis.modules.service.repository.ServiceResourceRepository;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.repository.VendorRepository;
import com.myapp.kinesis.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final VendorRepository vendorRepository;
    private final ResourceRepository resourceRepository;
    private final ServiceResourceRepository serviceResourceRepository;
    private final TenantContext tenantContext;

    public ServiceService(ServiceRepository serviceRepository,
                          VendorRepository vendorRepository,
                          ResourceRepository resourceRepository,
                          ServiceResourceRepository serviceResourceRepository,
                          TenantContext tenantContext) {
        this.serviceRepository = serviceRepository;
        this.vendorRepository = vendorRepository;
        this.resourceRepository = resourceRepository;
        this.serviceResourceRepository = serviceResourceRepository;
        this.tenantContext = tenantContext;
    }

    /**
     * Creates a new service and links it to its resources.
     * This is a transactional operation.
     */
    @Transactional
    public ServiceResponseDto createService(ServiceRequestDto createDto) {
        // 1. Get the vendorId from the "backpack"
        UUID vendorId = tenantContext.getVendorId();

        VendorEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", vendorId));

        // 2. Create the new ServiceEntity
        ServiceEntity newService = new ServiceEntity();
        newService.setVendor(vendor);
        newService.setName(createDto.name());
        newService.setDescription(createDto.description());
        newService.setDurationMinutes(createDto.durationMinutes());
        newService.setPrice(createDto.price());
        newService.setActive(createDto.isActive());

        //new fields
     // NEW FIELDS
        newService.setImageUrl(createDto.imageUrl());
        newService.setCategory(createDto.category());
        newService.setDisplayOrder(createDto.displayOrder() != null ? createDto.displayOrder() : 0);
        newService.setCurrency(createDto.currency() != null ? createDto.currency() : "INR");
        
        
        if (createDto.metadata() != null) {
            newService.setMetadata(createDto.metadata().toString());
        }
        ServiceEntity savedService = serviceRepository.save(newService);

        // 3. Link the required resources
        List<ServiceResourceEntity> resourceLinks = linkResourcesToService(
                savedService, createDto.resourceIds(), vendorId
        );

        // 4. Return the full DTO
        return ServiceResponseDto.fromEntity(savedService, resourceLinks);
    }

    /**
     * Helper method to link resources, ensuring they all belong to the same vendor.
     */
    private List<ServiceResourceEntity> linkResourcesToService(ServiceEntity service, List<UUID> resourceIds, UUID vendorId) {
        // TODO: Clear existing links if this is an "update" operation.

        return resourceIds.stream()
                .map(resourceId -> {
                    // Find the resource
                    ResourceEntity resource = resourceRepository.findById(resourceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", resourceId));

                    // CRITICAL: Check that the resource belongs to this vendor
                    if (!resource.getVendor().getId().equals(vendorId)) {
                        throw new SecurityException("Cannot link resource from another vendor.");
                    }

                    // Create the link
                    ServiceResourceEntity link = new ServiceResourceEntity(service, resource);
                    return serviceResourceRepository.save(link);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lists all services (active or inactive) for the *currently authenticated vendor*.
     * Used by the Admin PWA.
     */
    @Transactional(readOnly = true)
    public List<ServiceResponseDto> getServicesForCurrentVendor() {
        UUID vendorId = tenantContext.getVendorId();

        return serviceRepository.findAllByVendorId(vendorId).stream()
                .map(service -> {
                    // This is an N+1 query. We will fix this later with an optimized query.
                    List<ServiceResourceEntity> links = serviceResourceRepository.findAllByServiceId(service.getId());
                    return ServiceResponseDto.fromEntity(service, links);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lists all *active* services for a *public* vendor slug.
     * Used by the Public Website.
     */
    @Transactional(readOnly = true)
    public List<ServiceResponseDto> getPublicServicesForVendor(String slug) {
        // 1. Find the vendor by their public slug
        VendorEntity vendor = vendorRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "slug", slug));

     // Find active services and sort by displayOrder
        return serviceRepository.findAllByVendorIdAndIsActiveTrue(vendor.getId()).stream()
            .sorted((s1, s2) -> {
                int order1 = s1.getDisplayOrder() != null ? s1.getDisplayOrder() : 0;
                int order2 = s2.getDisplayOrder() != null ? s2.getDisplayOrder() : 0;
                return Integer.compare(order1, order2);
            })
            .map(service -> {
                List<ServiceResourceEntity> links = serviceResourceRepository.findAllByServiceId(service.getId());
                return ServiceResponseDto.fromEntity(service, links);
            })
            .collect(Collectors.toList());
    }
}