package com.myapp.kinesis.modules.service.entity;

import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * This is the "link" or "pivot" table entity.
 * It maps a many-to-many relationship between a Service and
 * the Resources it requires.
 * <p>
 * Example: "Teeth Cleaning" (Service) requires
 * - "Dr. Smith" (Resource)
 * - "Exam Room 1" (Resource)
 */
@Entity
@Table(name = "service_resources", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_service_resource_jpa",
                columnNames = {"service_id", "resource_id"}
        )
})
@Data
@NoArgsConstructor
public class ServiceResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_resource_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    public ServiceResourceEntity(ServiceEntity service, ResourceEntity resource) {
        this.service = service;
        this.resource = resource;
    }
}