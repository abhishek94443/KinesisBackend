package com.myapp.kinesis.modules.customer.service;

import com.myapp.kinesis.modules.customer.repository.ClientRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * This service implements UserDetailsService for the LOW-SECURITY "Air-Gap".
 * <p>
 * v2 (Corrected): The loadUserByUsername method MUST throw
 * UsernameNotFoundException (which is an AuthenticationException)
 * instead of UnsupportedOperationException (a RuntimeException).
 * This allows the global AuthenticationManager to fail gracefully and
 * try the next provider (i.e., StaffUserDetailsService).
 */
@Service("clientUserDetailsService") // A unique name for this bean
public class ClientUserDetailsService implements UserDetailsService {

    private final ClientRepository clientRepository;

    public ClientUserDetailsService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * This is the standard Spring interface.
     * The global AuthenticationManager will call this. We MUST NOT throw
     * a RuntimeException here.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // We fail gracefully. This tells the AuthenticationManager
        // "I didn't find this user in the 'clients' table. Please
        // try your other providers."
        throw new UsernameNotFoundException("ClientUserDetailsService cannot find user: " + email);
    }

    /**
     * This is our CUSTOM load method that the 'ClientAuthService' will use.
     * It correctly finds a customer using their email AND their vendor's ID.
     */
    public UserDetails loadClientByEmailAndVendor(String email, UUID vendorId) throws UsernameNotFoundException {
        return clientRepository.findByVendorIdAndEmail(vendorId, email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Client user not found with email '" + email + "' at this vendor.")
                );
    }
}