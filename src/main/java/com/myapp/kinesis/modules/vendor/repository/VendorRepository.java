package com.myapp.kinesis.modules.vendor.repository;

import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the VendorEntity.
 * This interface provides all standard CRUD methods for the "vendors" table.
 */
@Repository
public interface VendorRepository extends JpaRepository<VendorEntity, UUID> {

    /**
     * Finds a vendor by its public, unique 'slug' (e.g., "clinic-a").
     * This is a critical, high-performance query used by our
     * StaffAuthService and ClientAuthService during login.
     * The index 'idx_vendors_slug_unique' in our schema will keep this fast.
     *
     * @param slug The URL-friendly slug to search for.
     * @return An Optional containing the VendorEntity if found.
     */
    Optional<VendorEntity> findBySlug(String slug);

    /**
     * Checks if a vendor with this slug already exists.
     * This is used by the 'registerVendor' flow to prevent duplicates.
     *
     * @param slug The URL-friendly slug to check.
     * @return true if a vendor with this slug exists, false otherwise.
     */
    boolean existsBySlug(String slug);
}