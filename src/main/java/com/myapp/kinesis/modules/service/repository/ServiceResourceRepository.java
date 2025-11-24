package com.myapp.kinesis.modules.service.repository;

import com.myapp.kinesis.modules.service.entity.ServiceResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the ServiceResourceEntity (the "link" table).
 */
@Repository
public interface ServiceResourceRepository extends JpaRepository<ServiceResourceEntity, UUID> {

    /**
     * Finds all the resource links for a single service.
     * This is used by the AppointmentService to know which
     * resources (e.g., "Dr. Smith" and "Room 1") must be
     * available to book this service.
     */
    List<ServiceResourceEntity> findAllByServiceId(UUID serviceId);
}