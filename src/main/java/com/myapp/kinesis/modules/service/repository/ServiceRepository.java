package com.myapp.kinesis.modules.service.repository;

import com.myapp.kinesis.modules.service.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the ServiceEntity.
 */
@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {

    /**
     * Finds all active services for a specific vendor.
     * This is used to populate the public "menu" for a vendor's portal.
     */
	  // Add ordered query
    @Query("SELECT s FROM ServiceEntity s WHERE s.vendor.id = :vendorId AND s.isActive = true ORDER BY s.displayOrder ASC, s.name ASC")
    List<ServiceEntity> findAllByVendorIdAndIsActiveTrue(@Param("vendorId") UUID vendorId);

    /**
     * Finds all services (active or inactive) for a specific vendor.
     * This is used by the admin PWA to show the full list for editing.
     */
    List<ServiceEntity> findAllByVendorId(UUID vendorId);
    
    
    
}