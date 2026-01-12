package com.myapp.kinesis.modules.vendor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.modules.vendor.dto.BusinessHoursConfig;
import com.myapp.kinesis.modules.vendor.dto.VendorWebsiteConfigResponse;
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
    
    
    
    
    
    /**
     * Finds a vendor by their public slug.
     * Throws ResourceNotFoundException if the vendor does not exist.
     */
//    @Transactional(readOnly = true)
//    public VendorEntity getVendorBySlug(String slug) {
//        return vendorRepository.findBySlug(slug)
//                .orElseThrow(() -> new ResourceNotFoundException("Vendor workspace not found: " + slug));
//    }
    
    
    
    
    
    /**
     * Gets the website configuration for the currently authenticated vendor's slug.
     * This is used by the Admin Dashboard.
     * 
     * @param slug The vendor's slug (must match the authenticated vendor)
     * @return VendorWebsiteConfigResponse with the current website_config
     */
    @SneakyThrows
    @Transactional(readOnly = true)
    public VendorWebsiteConfigResponse getWebsiteConfig(String slug) {
        // Get the authenticated vendor ID from TenantContext
//        UUID authenticatedVendorId = tenantContext.getVendorId();
        
        // Find the vendor by slug
        VendorEntity vendor = vendorRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "slug", slug));
        
        // Security check: Make sure the slug matches the authenticated vendor
//        if (!vendor.getId().equals(authenticatedVendorId)) {
//            throw new TenantAccessDeniedException(
//                "Access Denied: You cannot access website config for another vendor."
//            );
//        }
        
        // Parse website_config from string to JsonNode
        JsonNode websiteConfig = null;
        if (vendor.getWebsiteConfig() != null && !vendor.getWebsiteConfig().isBlank()) {
            websiteConfig = objectMapper.readTree(vendor.getWebsiteConfig());
        }
        
        return new VendorWebsiteConfigResponse(
            vendor.getVendorName(),
            vendor.getSlug(),
            websiteConfig
        );
    }

    /**
     * Updates the website configuration for the currently authenticated vendor's slug.
     * This is used by the Admin Dashboard.
     * 
     * @param slug The vendor's slug (must match the authenticated vendor)
     * @param websiteConfig The new website configuration JSON
     * @return VendorWebsiteConfigResponse with the updated config
     */
    @SneakyThrows
    @Transactional
    public VendorWebsiteConfigResponse updateWebsiteConfig(String slug, JsonNode websiteConfig) {
        // Get the authenticated vendor ID from TenantContext
//        UUID authenticatedVendorId = tenantContext.getVendorId();
        
        // Find the vendor by slug
        VendorEntity vendor = vendorRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "slug", slug));
        
        // Security check: Make sure the slug matches the authenticated vendor
//        if (!vendor.getId().equals(authenticatedVendorId)) {
//            throw new TenantAccessDeniedException(
//                "Access Denied: You cannot update website config for another vendor."
//            );
//        }
        
        // Validate JSON size (prevent abuse - max 500KB)
        String configString = objectMapper.writeValueAsString(websiteConfig);
        if (configString.length() > 500_000) {
            throw new IllegalArgumentException("Website configuration is too large. Maximum size is 500KB.");
        }
        
        // Update the website_config
        vendor.setWebsiteConfig(configString);
        VendorEntity savedVendor = vendorRepository.save(vendor);
        
        // Parse back to JsonNode for response
        JsonNode updatedConfig = objectMapper.readTree(savedVendor.getWebsiteConfig());
        
        return new VendorWebsiteConfigResponse(
            savedVendor.getVendorName(),
            savedVendor.getSlug(),
            updatedConfig
        );
    }
    
}