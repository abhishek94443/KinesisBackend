package com.myapp.kinesis.modules.vendor.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * DTO for returning vendor website configuration.
 */
public record VendorWebsiteConfigResponse(
    String vendorName,
    String slug,
    JsonNode websiteConfig
) {}