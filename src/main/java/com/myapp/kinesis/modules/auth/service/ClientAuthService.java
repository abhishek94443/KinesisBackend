package com.myapp.kinesis.modules.auth.service;

import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.common.exceptions.TenantAccessDeniedException;
import com.myapp.kinesis.config.JwtService;
import com.myapp.kinesis.modules.auth.dto.ClientLoginRequest;
import com.myapp.kinesis.modules.auth.dto.ClientRegistrationRequest;
import com.myapp.kinesis.modules.auth.dto.LoginResponse;
import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import com.myapp.kinesis.modules.customer.repository.ClientRepository;
import com.myapp.kinesis.modules.customer.service.ClientUserDetailsService;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v3 (Corrected):
 * This service must NOT use the global AuthenticationManager.
 * It must *manually* load the user from the correct silo (using
 * ClientUserDetailsService) and *then* check the password.
 * This is the only way to do a "Contextual Login" for a
 * non-globally-unique user.
 */
@Service
public class ClientAuthService {

    private final ClientRepository clientRepository;
    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ClientUserDetailsService clientUserDetailsService;

    public ClientAuthService(ClientRepository clientRepository,
                             VendorRepository vendorRepository,
                             PasswordEncoder passwordEncoder,
                             JwtService jwtService,
                             @Qualifier("clientUserDetailsService") ClientUserDetailsService clientUserDetailsService) {
        this.clientRepository = clientRepository;
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clientUserDetailsService = clientUserDetailsService;
    }

    @Transactional
    public ClientEntity registerClient(ClientRegistrationRequest request) {

        VendorEntity vendor = vendorRepository.findBySlug(request.vendorSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with slug: " + request.vendorSlug()));

        UUID vendorId = vendor.getId();

        if (clientRepository.existsByVendorIdAndEmail(vendorId, request.email())) {
            throw new IllegalArgumentException("Error: Email is already in use at this location.");
        }

        ClientEntity newClient = new ClientEntity();
        newClient.setVendor(vendor);
        newClient.setEmail(request.email());
        newClient.setPassword(passwordEncoder.encode(request.password()));
        newClient.setFirstName(request.firstName());
        newClient.setLastName(request.lastName());
        newClient.setPhone(request.phone());
        newClient.setStatus("ACTIVE");

        return clientRepository.save(newClient);
    }

    /**
     * Authenticates a CLIENT (patient/member) for a specific vendor portal.
     * This logic is *manual* because the user is not globally unique.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(ClientLoginRequest request) {

        // 1. Find the Vendor "Silo"
        VendorEntity vendor = vendorRepository.findBySlug(request.vendorSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with slug: " + request.vendorSlug()));

        UUID vendorId = vendor.getId();

        // 2. Manually load the user from the correct "silo"
        UserDetails userDetails = clientUserDetailsService.loadClientByEmailAndVendor(request.email(), vendorId);

        // 3. Manually check the password
        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new TenantAccessDeniedException("Invalid email or password.");
        }

        ClientEntity client = (ClientEntity) userDetails;
        String contextualRole = "ROLE_CUSTOMER";

        // 4. Generate the token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", client.getId().toString()); // This is the Client ID
        claims.put("vendorId", vendorId.toString());
        claims.put("role", contextualRole);

        String jwt = jwtService.generateToken(claims, client);
        return new LoginResponse(
                jwt,
                contextualRole,
                client.getEmail(),
                client.getId(),
                vendorId,
                vendor.getVendorName(), // <--- Correct Business Name
                vendor.getSlug()        // <--- Correct Slug
        );
//        return new LoginResponse(jwt, contextualRole, client.getEmail(), client.getId(), vendorId);
    }
}