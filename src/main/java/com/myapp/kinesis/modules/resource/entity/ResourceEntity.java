package com.myapp.kinesis.modules.resource.entity;

import com.myapp.kinesis.common.enums.ResourceType;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Represents a "bookable asset" (e.g., a person, place, or thing)
 * belonging to a specific vendor.
 * Maps to the 'resources' table.
 */
@Entity
@Table(name = "resources", indexes = {
        // We must add an index to 'vendor_id' for fast lookups
        @Index(name = "idx_resources_vendor_id", columnList = "vendor_id")
})
@Data
@NoArgsConstructor
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Application (Java/JPA) generates the key
    @Column(name = "resource_id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The Vendor (tenant) that "owns" this resource.
     * This is the foreign key for data isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorEntity vendor;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ResourceType type;

    /**
     * This is the "flexibility" column.
     * It stores resource-specific, unstructured data.
     * - For STAFF: { "title": "Senior Dentist", "bio": "..." }
     * - For ROOM: { "location": "Floor 2, West Wing" }
     * - For EQUIPMENT: { "serial_number": "SN-12345", "last_serviced": "2025-10-01" }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // Stored as a raw JSON string

    // Helper constructor for our ResourceService
    public ResourceEntity(VendorEntity vendor, String name, ResourceType type) {
        this.vendor = vendor;
        this.name = name;
        this.type = type;
    }
}