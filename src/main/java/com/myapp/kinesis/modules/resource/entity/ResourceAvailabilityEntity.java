package com.myapp.kinesis.modules.resource.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Represents a time period when a resource is UNAVAILABLE.
 * (e.g., "Dr. Smith is on vacation Dec 10-17").
 */
@Entity
@Table(name = "resource_availability", indexes = {
        @Index(name = "idx_resource_avail_time", columnList = "resource_id, unavailable_start, unavailable_end")
})
@Data
@NoArgsConstructor
public class ResourceAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "availability_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    @Column(name = "unavailable_start", nullable = false)
    private OffsetDateTime unavailableStart;

    @Column(name = "unavailable_end", nullable = false)
    private OffsetDateTime unavailableEnd;

    @Column(name = "reason")
    private String reason; // "VACATION", "SICK_LEAVE", "MAINTENANCE"

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}