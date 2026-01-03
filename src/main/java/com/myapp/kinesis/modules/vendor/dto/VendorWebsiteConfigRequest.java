package com.myapp.kinesis.modules.vendor.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating vendor website configuration.
 */
public record VendorWebsiteConfigRequest(
    @NotNull(message = "Website configuration is required")
    JsonNode websiteConfig
) {}