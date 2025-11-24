package com.myapp.kinesis.modules.customer.service;

import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import com.myapp.kinesis.modules.customer.repository.ClientRepository;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Client (customer/patient) profiles.
 */
@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final VendorRepository vendorRepository;

    public ClientService(ClientRepository clientRepository, VendorRepository vendorRepository) {
        this.clientRepository = clientRepository;
        this.vendorRepository = vendorRepository;
    }

    /**
     * This is the "Guest Booking" logic.
     * It finds a guest customer by their email or creates a new, un-authenticated
     * guest profile for them.
     *
     * @param vendorId  The vendor silo this guest belongs to.
     * @param email     The guest's email.
     * @param firstName The guest's first name.
     * @param phone     The guest's phone.
     * @return A 'ClientEntity' (either existing or new).
     */
    @Transactional
    public ClientEntity findOrCreateGuestClient(UUID vendorId, String email, String firstName, String lastName, String phone) {

        // 1. Try to find an existing customer profile *at this vendor*
        Optional<ClientEntity> existingClient = clientRepository.findByVendorIdAndEmail(vendorId, email);

        if (existingClient.isPresent()) {
            // Found them. Return the existing profile.
            return existingClient.get();
        } else {
            // 2. Not found. Create a new "Guest" profile.

            // Get the Vendor reference
            VendorEntity vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", vendorId));

            ClientEntity newGuest = new ClientEntity();
            newGuest.setVendor(vendor);
            newGuest.setFirstName(firstName);
            newGuest.setLastName(lastName);
            newGuest.setEmail(email);
            newGuest.setPhone(phone);
            newGuest.setStatus("ACTIVE");
            // newGuest.setUser(null) is default (this is a GUEST)
            // newGuest.setPassword(null) is default (no login)

            return clientRepository.save(newGuest);
        }
    }
}