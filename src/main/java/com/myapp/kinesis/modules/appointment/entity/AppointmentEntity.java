package com.myapp.kinesis.modules.appointment.entity;

import com.myapp.kinesis.common.enums.AppointmentStatus;
import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import com.myapp.kinesis.modules.service.entity.ServiceEntity;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the core "Appointment" transaction.
 * Maps to the 'appointments' table.
 * <p>
 * v2 (Corrected): Added the missing @OneToMany 'appointmentResources'
 * relationship, which is required for our 'findConflictingAppointments'
 * query in the repository.
 */
@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appts_vendor_id_start_time_jpa", columnList = "vendor_id, start_time"),
        @Index(name = "idx_appts_client_id_jpa", columnList = "client_id")
})
@Data
@NoArgsConstructor
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appointment_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorEntity vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    // --- THIS IS THE FIX ---
    /**
     * This is the "link" to the 'appointment_resources' table.
     * It defines the list of resources (e.g., "Dr. Smith", "Room 1")
     * that are reserved for this one appointment.
     * <p>
     * - @OneToMany: One Appointment has Many resource links.
     * - mappedBy = "appointment": Tells JPA that the 'AppointmentResourceEntity'
     * class is the "owner" of this relationship, in its field named "appointment".
     * - cascade = CascadeType.ALL: When we save an Appointment, also save
     * all the 'AppointmentResourceEntity' objects in this list.
     * - orphanRemoval = true: If we remove a resource from this list,
     * delete it from the database.
     */
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Exclude from Lombok's toString to prevent infinite loops
    @EqualsAndHashCode.Exclude // Exclude from Lombok's equals/hashCode
    private List<AppointmentResourceEntity> appointmentResources = new ArrayList<>();

    // -------------------------

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "price_charged")
    private BigDecimal priceCharged;

    @Column(name = "booking_contact_name", nullable = false)
    private String bookingContactName;

    @Column(name = "booking_contact_email")
    private String bookingContactEmail;

    @Column(name = "booking_contact_phone")
    private String bookingContactPhone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}