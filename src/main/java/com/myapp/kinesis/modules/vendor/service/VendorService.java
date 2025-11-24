package com.myapp.kinesis.modules.vendor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.modules.vendor.dto.BusinessHoursConfig;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.repository.VendorRepository;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VendorService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }


    @SneakyThrows
    @Transactional(readOnly = true)
    public BusinessHoursConfig getBusinessHours(UUID vendorId) {
        VendorEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", vendorId));

        if (vendor.getSettings() == null || vendor.getSettings().isBlank()) {
            return BusinessHoursConfig.getDefault();
        }

        JsonNode settings = objectMapper.readTree(vendor.getSettings());

        if (!settings.has("business_hours")) {
            return BusinessHoursConfig.getDefault();
        }

        // Parse the business_hours JSON into our config object
        return objectMapper.treeToValue(settings, BusinessHoursConfig.class);
    }

    /**
     * Finds a vendor by their public slug.
     * Throws ResourceNotFoundException if the vendor does not exist.
     */
    @Transactional(readOnly = true)
    public VendorEntity getVendorBySlug(String slug) {
        return vendorRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor workspace not found: " + slug));
    }
}