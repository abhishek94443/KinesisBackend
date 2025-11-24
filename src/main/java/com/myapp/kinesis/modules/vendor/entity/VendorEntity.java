package com.myapp.kinesis.modules.vendor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Represents a Vendor (a tenant) in the system (e.g., a clinic, a gym).
 * This entity maps to the 'vendors' table.
 * This is the "silo" that holds all of a customer's data.
 */
@Entity
@Table(name = "vendors")
@Data
@NoArgsConstructor
public class VendorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Application (Java/JPA) generates the key
    @Column(name = "vendor_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    /**
     * The unique, URL-friendly slug for this vendor.
     * This is the public identifier (e.g., "city-central-dentistry").
     */
    @Column(name = "slug", unique = true, nullable = false)
    private String slug;
    // NEW: Stores the JSON configuration for their website
    // We use 'columnDefinition = "jsonb"' for PostgreSQL
    @Column(name = "website_config", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String websiteConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * @PrePersist is a JPA callback hook.
     * This method is automatically run by JPA just before a new
     * VendorEntity is first saved (persisted) to the database.
     * We use it to reliably set the creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            // We force all timestamps to UTC to prevent time zone bugs.
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    // Helper constructor for our AuthService
    public VendorEntity(String vendorName, String slug) {
        this.vendorName = vendorName;
        this.slug = slug;
    }


}