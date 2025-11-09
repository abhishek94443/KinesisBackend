package com.myapp.kinesis.modules.staff.entity;

import com.myapp.kinesis.common.enums.VendorStaffRole;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents the "staff_roles" table (the "Permission" table).
 * This entity maps to our SQL schema (v6), which uses a
 * single UUID "surrogate key" (role_id) as its Primary Key.
 * <p>
 * We enforce the business rule ("one user per vendor") using a
 *
 * @UniqueConstraint at the @Table level.
 */
@Entity
@Table(name = "staff_roles", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_staff_vendor_role",
                columnNames = {"staff_id", "vendor_id"} // Enforces our UNIQUE("staff_id", "vendor_id") rule
        )
})
@Data
@NoArgsConstructor
public class StaffRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id", updatable = false, nullable = false)
    private UUID id;

    /**
     * This is a simple ManyToOne relationship.
     * It maps to the "staff_id" foreign key column.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffEntity staff;

    /**
     * This is a simple ManyToOne relationship.
     * It maps to the "vendor_id" foreign key column.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorEntity vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private VendorStaffRole role;

    /**
     * Helper constructor to make creation easier in our AuthService.
     */
    public StaffRoleEntity(StaffEntity staff, VendorEntity vendor, VendorStaffRole role) {
        this.staff = staff;
        this.vendor = vendor;
        this.role = role;
    }
}