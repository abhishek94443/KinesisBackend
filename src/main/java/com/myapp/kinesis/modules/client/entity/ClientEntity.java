package com.myapp.kinesis.modules.customer.entity;

import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Client (a vendor's customer, e.g., patient, member).
 * This entity maps to the 'clients' table and serves as BOTH the
 * customer profile AND their login (UserDetails).
 * This is the "low-security" identity in our "Air-Gap" model.
 */
@Entity
@Table(name = "clients", indexes = {
        // This replicates the UNIQUE constraints from our schema
        // to prevent two clients with the same email/phone *at the same vendor*.
        @Index(name = "idx_client_vendor_email_unique", columnList = "vendor_id, email", unique = true),
        @Index(name = "idx_client_vendor_phone_unique", columnList = "vendor_id, phone", unique = true)
})
@Data
@NoArgsConstructor
public class ClientEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "client_id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The Vendor (tenant) that "owns" this client record.
     * This is the primary key for all isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorEntity vendor;

    // --- AUTHENTICATION FIELDS (Nullable) ---
    // A guest profile (created by staff) might not have a login.

    @Column(name = "email") // Only unique *per vendor*
    private String email;

    @Column(name = "hashed_password")
    private String password;

    // --- PROFILE FIELDS ---

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // e.g., 'ACTIVE', 'BLOCKED_BY_VENDOR'

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // Stores clinic data { "dob" } or gym data { "membership_id" }

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    // --- UserDetails Implementation (for Customer Security) ---
    // A ClientEntity can *also* be a UserDetails object.

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // All clients, when logged in, get the "ROLE_CUSTOMER".
        return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Override
    public String getUsername() {
        return this.email; // Customer logs in with their email
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // If a vendor blocks a client, we can lock their account here.
        return !"BLOCKED_BY_VENDOR".equals(this.status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // A client account is only "enabled" if it has a password.
        // A guest profile (password=null) cannot be used to log in.
        return this.password != null;
    }
}