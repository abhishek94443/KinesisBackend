//package com.myapp.kinesis.modules.staff.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.util.Collection;
//import java.util.List;
//import java.util.UUID;
//
///**
// * Represents the HIGH-SECURITY 'staff' table (Admins, Vendor Owners, Staff).
// * This is the "Admin Identity" and is completely separate from the 'clients' table.
// * It implements UserDetails to integrate with our Admin-only SecurityConfig.
// */
//@Entity
//@Table(name = "staff")
//@Data
//@NoArgsConstructor
//public class StaffEntity implements UserDetails {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    @Column(name = "staff_id", updatable = false, nullable = false)
//    private UUID id;
//
//    @Column(unique = true, nullable = false)
//    private String email;
//
//    @Column(name = "hashed_password", nullable = false)
//    private String password; // Field named 'password' to match UserDetails
//
//    @Column(name = "is_superadmin", nullable = false)
//    private boolean isSuperadmin = false;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private OffsetDateTime createdAt;
//
//    @PrePersist
//    protected void onCreate() {
//        if (createdAt == null) {
//            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
//        }
//    }
//
//    // --- UserDetails Implementation (for Spring Security) ---
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        // This is a base role. The *contextual* role (VENDOR_OWNER, etc.)
//        // will be put in the JWT by the AuthService.
//        if (this.isSuperadmin) {
//            return List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
//        }
//        return List.of(new SimpleGrantedAuthority("ROLE_STAFF_MEMBER"));
//    }
//
//    @Override
//    public String getUsername() {
//        return this.email; // We use email as the username
//    }
//
//    @Override
//    public String getPassword() {
//        return this.password;
//    }
//
//    // --- We can build logic for these later ---
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return true;
//    }
//}


package com.myapp.kinesis.modules.staff.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "staff")
@Data
@NoArgsConstructor
public class StaffEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "staff_id", updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "hashed_password", nullable = false)
    private String password;

    // --- NEW FIELDS (Human Identity) ---
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;
    // -----------------------------------

    @Column(name = "is_superadmin", nullable = false)
    private boolean isSuperadmin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.isSuperadmin) {
            return List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_STAFF_MEMBER"));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}