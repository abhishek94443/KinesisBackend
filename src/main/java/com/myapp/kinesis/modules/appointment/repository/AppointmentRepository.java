package com.myapp.kinesis.modules.appointment.repository;

import com.myapp.kinesis.common.enums.AppointmentStatus;
import com.myapp.kinesis.modules.appointment.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * v2 (Completed): Added all missing query methods for MVP.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    /**
     * CRITICAL FIX: Finds all appointments for a vendor.
     * Used by the admin dashboard.
     */
    List<AppointmentEntity> findAllByVendorId(UUID vendorId);

    /**
     * CRITICAL FIX: Finds conflicting appointments for resource booking.
     * This query checks if ANY of the required resources are already booked
     * during the requested time window.
     */
    @Query("""
            SELECT DISTINCT a FROM AppointmentEntity a
            JOIN a.appointmentResources ar
            WHERE a.vendor.id = :vendorId
              AND ar.resource.id IN :resourceIds
              AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
              AND (
                (a.startTime < :endTime AND a.endTime > :startTime)
              )
            """)
    List<AppointmentEntity> findConflictingAppointments(
            @Param("vendorId") UUID vendorId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("resourceIds") List<UUID> resourceIds
    );

    /**
     * Counts existing bookings for a service at a specific time.
     * Used for GROUP capacity checking.
     */
    int countByServiceIdAndStartTimeAndStatusNot(
            UUID serviceId,
            OffsetDateTime startTime,
            AppointmentStatus status
    );

    /**
     * NEW: Finds all QUEUE appointments (for walk-in businesses).
     * These have startTime = NULL and are sorted by created_at.
     */
    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.vendor.id = :vendorId
              AND a.service.id = :serviceId
              AND a.startTime IS NULL
              AND a.status = 'PENDING'
            ORDER BY a.createdAt ASC
            """)
    List<AppointmentEntity> findQueueByVendorAndService(
            @Param("vendorId") UUID vendorId,
            @Param("serviceId") UUID serviceId
    );

    /**
     * NEW: Finds the first waitlisted appointment for a service/time.
     * Used when a cancellation opens up a slot.
     */
    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.service.id = :serviceId
              AND a.startTime = :startTime
              AND a.status = 'WAITLISTED'
            ORDER BY a.createdAt ASC
            LIMIT 1
            """)
    AppointmentEntity findFirstWaitlisted(
            @Param("serviceId") UUID serviceId,
            @Param("startTime") OffsetDateTime startTime
    );
}