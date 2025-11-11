package com.myapp.kinesis.modules.auth.service;

import com.myapp.kinesis.common.enums.VendorStaffRole;
import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.common.exceptions.TenantAccessDeniedException;
import com.myapp.kinesis.config.JwtService;
import com.myapp.kinesis.modules.auth.dto.LoginResponse;
import com.myapp.kinesis.modules.auth.dto.StaffLoginRequest;
import com.myapp.kinesis.modules.auth.dto.VendorRegistrationRequest;
import com.myapp.kinesis.modules.staff.entity.StaffEntity;
import com.myapp.kinesis.modules.staff.entity.StaffRoleEntity;
import com.myapp.kinesis.modules.staff.repository.StaffRepository;
import com.myapp.kinesis.modules.staff.repository.StaffRoleRepository;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v3 (Corrected):
 * Injects the *specific* 'staffAuthenticationProvider' instead of the
 * global AuthenticationManager to enforce the "Air-Gap".
 */
@Service
public class StaffAuthService {

    // --- We inject the SPECIFIC provider, not the global manager ---
    private final AuthenticationProvider authenticationProvider;
    private final StaffRepository staffRepository;
    private final VendorRepository vendorRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public StaffAuthService(
            @Qualifier("staffAuthenticationProvider") AuthenticationProvider authenticationProvider,
            StaffRepository staffRepository,
            VendorRepository vendorRepository,
            StaffRoleRepository staffRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.authenticationProvider = authenticationProvider;
        this.staffRepository = staffRepository;
        this.vendorRepository = vendorRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public StaffEntity registerVendor(VendorRegistrationRequest request) {

        if (staffRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }
        if (vendorRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Error: This portal URL (slug) is already taken.");
        }
        StaffEntity newStaff = new StaffEntity();
        newStaff.setEmail(request.email());
        newStaff.setPassword(passwordEncoder.encode(request.password()));
        newStaff.setSuperadmin(false);
        staffRepository.save(newStaff);

        VendorEntity newVendor = new VendorEntity(request.vendorName(), request.slug());
        vendorRepository.save(newVendor);

        StaffRoleEntity ownerRole = new StaffRoleEntity(
                newStaff,
                newVendor,
                VendorStaffRole.VENDOR_OWNER
        );
        staffRoleRepository.save(ownerRole);

        return newStaff;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(StaffLoginRequest request) {

        // 1. Authenticate (Global): Check email and password
        // We now call our *specific* staff-only provider.
        // This will ONLY use StaffUserDetailsService and will NEVER
        // touch the ClientUserDetailsService. This fixes the bug.
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        StaffEntity staff = (StaffEntity) authentication.getPrincipal();

        // 2. Find the Vendor "Silo" using the slug
        VendorEntity vendor = vendorRepository.findBySlug(request.vendorSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with slug: " + request.vendorSlug()));

        UUID vendorId = vendor.getId();

        // 3. Authorize (Contextual): Find the user's role *at this vendor*
        StaffRoleEntity staffRole = staffRoleRepository.findByStaffIdAndVendorId(staff.getId(), vendorId)
                .orElseThrow(() ->
                        new TenantAccessDeniedException("User is not authorized for this vendor admin portal.")
                );

        String contextualRole = "ROLE_" + staffRole.getRole().name();

        // 4. Generate a JWT *with* the contextual role and vendorId
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", staff.getId().toString());
        claims.put("vendorId", vendorId.toString());
        claims.put("role", contextualRole);

        String jwt = jwtService.generateToken(claims, staff);

        return new LoginResponse(jwt, contextualRole, staff.getEmail(), staff.getId(), vendorId);
    }
}