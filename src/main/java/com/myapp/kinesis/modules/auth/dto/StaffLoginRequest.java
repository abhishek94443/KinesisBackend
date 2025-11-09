package com.myapp.kinesis.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO record for a VENDOR_OWNER or STAFF logging in.
 * This is used by StaffAuthController.
 */
public record StaffLoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        /**
         * The unique, human-readable slug of the vendor (e.g., "clinic-a").
         * The PWA frontend will get this from its subdomain (clinic-a.kinesis.io).
         */
        @NotBlank(message = "Vendor portal slug is required for login")
        String vendorSlug
) {
}