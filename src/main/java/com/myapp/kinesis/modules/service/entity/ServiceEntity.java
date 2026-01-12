package com.myapp.kinesis.modules.service.entity;

import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a "Service" or "Product" that a vendor offers.
 * (e.g., "Teeth Cleaning", "Yoga Class", "1-Hour Legal Consultation").
 * This entity maps to the 'services' table.
 */
@Entity
@Table(name = "services", indexes = {
        @Index(name = "idx_services_vendor_id_jpa", columnList = "vendor_id")
})
@Data
@NoArgsConstructor
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorEntity vendor;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30; // Default to 30 minutes

    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    //new fix for frontend
    @Column(name = "image_url")
    private String imageUrl;  // NEW
    
    @Column(name = "category")
    private String category;  // NEW
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;  // NEW - for sorting
    
    @Column(name = "currency")
    private String currency = "INR";  // NEW - default INR
    
    /**
     * This is the "brain" of the service.
     * It stores all the business rules for this specific service.
     * <p>
     * Example:
     * {
     * "booking_model": "SCHEDULED",
     * "capacity": 25,
     * "billing_model": "SUBSCRIPTION",
     * "free_for_plans": ["gold_pass"]
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    private List<ServiceResourceEntity> serviceResources = new ArrayList<>();
}