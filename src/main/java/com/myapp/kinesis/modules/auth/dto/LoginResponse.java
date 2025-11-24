//package com.myapp.kinesis.modules.auth.dto;
//
//import java.util.UUID;
//
///**
// * DTO record for a successful login response (for both Staff and Clients).
// * This is the "passport" we send back, containing the JWT and user's context.
// */
//public record LoginResponse(
//
//        String token,
//        String role, // e.g., "ROLE_VENDOR_OWNER", "ROLE_STAFF", "ROLE_CUSTOMER"
//        String email,
//        UUID userId,  // This is the Staff ID or Client ID
//        UUID vendorId // The Vendor ID (silo) they are logged into
//) {
//}


package com.myapp.kinesis.modules.auth.dto;

import java.util.UUID;

/**
 * DTO record for a successful login response (for both Staff and Clients).
 * Updated to include Vendor Identity (Name & Slug) for the frontend UI.
 */
public record LoginResponse(

        String token,
        String role, // e.g., "ROLE_VENDOR_OWNER", "ROLE_STAFF"
        String email,
        UUID userId,
        UUID vendorId,

        // --- NEW FIELDS ---
        // These allow the Frontend to display "Raj Pharma" in the sidebar
        // instead of generic text or the user's email.
        String vendorName,
        String vendorSlug
) {
}