package com.myapp.kinesis.modules.staff.repository;

import com.myapp.kinesis.modules.staff.entity.StaffRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the StaffRoleEntity.
 * The Primary Key type is 'UUID' to match the 'role_id' field.
 */
@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRoleEntity, UUID> {

    /**
     * Finds a user's staff role at a specific vendor.
     * This is the core query for our "Contextual Login" logic.
     */
    Optional<StaffRoleEntity> findByStaffIdAndVendorId(UUID staffId, UUID vendorId);

    /**
     * Finds ALL roles a single staff member has across ALL vendors.
     */
    List<StaffRoleEntity> findAllByStaffId(UUID staffId);

    /**
     * Checks if a user is already linked to a vendor.
     */
    boolean existsByStaffIdAndVendorId(UUID staffId, UUID vendorId);
}