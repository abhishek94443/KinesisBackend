package com.myapp.kinesis.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) record for a new VENDOR signing up.
 * This is used by StaffAuthController.
 */
public record VendorRegistrationRequest(

        @NotBlank(message = "Business name is required")
        @Size(min = 2, message = "Business name must be at least 2 characters")
        String vendorName,

        @NotBlank(message = "Portal URL is required")
        @Size(min = 3, max = 30, message = "URL must be 3-30 characters")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "URL must be lowercase letters, numbers, and dashes (e.g., 'my-cool-clinic')")
        String slug,

        @NotBlank(message = "Your email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}