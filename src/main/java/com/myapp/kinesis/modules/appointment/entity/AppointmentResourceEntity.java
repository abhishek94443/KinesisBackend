package com.myapp.kinesis.modules.appointment.entity;

import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * This is the "link" or "pivot" table entity.
 * It "reserves" a Resource (like "Dr. Smith") for a
 * specific Appointment.
 */
@Entity
@Table(name = "appointment_resources", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_appointment_resource_jpa",
                columnNames = {"appointment_id", "resource_id"}
        )
})
@Data
@NoArgsConstructor
public class AppointmentResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appointment_resource_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private AppointmentEntity appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    public AppointmentResourceEntity(AppointmentEntity appointment, ResourceEntity resource) {
        this.appointment = appointment;
        this.resource = resource;
    }
}