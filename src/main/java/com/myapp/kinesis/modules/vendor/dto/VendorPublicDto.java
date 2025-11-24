package com.myapp.kinesis.modules.vendor.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * Public information about a vendor.
 * Updated to include the UUID so the frontend can fetch services.
 */
public record VendorPublicDto(

        String name,
        String slug,
        // NEW: The blueprint for their website
        @JsonRawValue
        String websiteConfig

) {
}