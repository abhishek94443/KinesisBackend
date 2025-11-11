package com.myapp.kinesis.modules.staff.service;

import com.myapp.kinesis.modules.staff.repository.StaffRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * This service implements UserDetailsService for the HIGH-SECURITY "Air-Gap".
 * Its *only* job is to load a user from the 'staff' table.
 * It will be used by our 'StaffSecurityConfig'.
 */
@Service("staffUserDetailsService") // We give it a unique name
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    public StaffUserDetailsService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    /**
     * This method is called by Spring Security's "Staff" AuthenticationProvider.
     *
     * @param email The email to look up in the 'staff' table.
     * @return UserDetails (our StaffEntity)
     * @throws UsernameNotFoundException if the email is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return staffRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Staff user not found with email: " + email)
                );
    }
}