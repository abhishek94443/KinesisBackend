package com.myapp.kinesis.modules.staff.repository;

import com.myapp.kinesis.modules.staff.entity.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the StaffEntity (the 'staff' table).
 */
@Repository
public interface StaffRepository extends JpaRepository<StaffEntity, UUID> {

    /**
     * Finds a staff user by their unique email address.
     * This is the primary method used by the staff login service.
     */
    Optional<StaffEntity> findByEmail(String email);

    /**
     * Checks if a staff user with this email already exists.
     * Used by the vendor registration service.
     */
    Boolean existsByEmail(String email);
}