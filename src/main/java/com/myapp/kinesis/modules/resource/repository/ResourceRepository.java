package com.myapp.kinesis.modules.resource.repository;

import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the ResourceEntity.
 */
@Repository
public interface ResourceRepository extends JpaRepository<ResourceEntity, UUID> {

    /**
     * Finds all resources that belong to a specific vendor.
     * This will be used by the PWA to populate the "Manage Resources" page.
     * The TenantInterceptor (ALT) and RLS will provide security,
     * but querying by vendorId is still a best practice.
     *
     * @param vendorId The UUID of the vendor.
     * @return A list of all resources for that vendor.
     */
    List<ResourceEntity> findAllByVendorId(UUID vendorId);
}