package com.myapp.kinesis.modules.customer.repository;

import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the ClientEntity.
 */
@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    /**
     * Finds a customer profile based on their email *and* the vendor they belong to.
     * This is the primary query for the CUSTOMER login flow.
     */
    Optional<ClientEntity> findByVendorIdAndEmail(UUID vendorId, String email);

    /**
     * Finds a customer profile based on their phone number *and* the vendor they belong to.
     */
    Optional<ClientEntity> findByVendorIdAndPhone(UUID vendorId, String phone);

    /**
     * Checks if a customer with this email already exists *at this vendor*.
     */
    boolean existsByVendorIdAndEmail(UUID vendorId, String email);

}