package com.myapp.kinesis.modules.resource.repository;

import com.myapp.kinesis.modules.resource.entity.ResourceAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceAvailabilityRepository extends JpaRepository<ResourceAvailabilityEntity, UUID> {

    /**
     * Checks if a resource is unavailable during a given time range.
     */
    @Query("""
            SELECT CASE WHEN COUNT(ra) > 0 THEN true ELSE false END
            FROM ResourceAvailabilityEntity ra
            WHERE ra.resource.id = :resourceId
              AND ra.unavailableStart < :endTime
              AND ra.unavailableEnd > :startTime
            """)
    boolean existsByResourceIdAndTimeRange(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Finds all unavailability periods for a resource.
     */
    List<ResourceAvailabilityEntity> findAllByResourceId(UUID resourceId);
}