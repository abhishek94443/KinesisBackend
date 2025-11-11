package com.myapp.kinesis.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO record for a new CLIENT (patient/member) signing up on a vendor's portal.
 * This is used by ClientAuthController.
 */
public record ClientRegistrationRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        String lastName,

        @NotBlank(message = "Phone number is required")
        String phone,

        /**
         * The unique, human-readable slug of the vendor (e.g., "clinic-a").
         * The PWA frontend will get this from its subdomain.
         */
        @NotBlank(message = "Vendor slug is required")
        String vendorSlug
) {
}