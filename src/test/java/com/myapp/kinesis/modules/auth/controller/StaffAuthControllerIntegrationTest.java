//package com.myapp.kinesis.modules.auth.controller;
//
//import com.myapp.kinesis.BaseIntegrationTest;
//import com.myapp.kinesis.modules.auth.dto.LoginResponse;
//import com.myapp.kinesis.modules.auth.dto.StaffLoginRequest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * v2: Refactored to extend BaseIntegrationTest.
// * This file *only* tests the auth logic.
// */
//public class StaffAuthControllerIntegrationTest extends BaseIntegrationTest {
//
//    // The @BeforeEach setup (registering vendors)
//    // is now *automatically* run from the BaseIntegrationTest.
//
//    @Test
//    @DisplayName("Test B: Staff Login (Happy Path) & TenantContext")
//    void testStaffLoginAndContext() throws Exception {
//        // --- Test B (Part 1): Login to Vendor A ---
//        // We use our new helper method from the base class
//        LoginResponse loginResponse = loginAsStaff(VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_A_SLUG);
//        String token = loginResponse.token();
//
//        // --- Test B (Part 2): Test the "Backpack" (TenantContext) ---
//        // This test requires StaffAdminController (and /context-test) to exist.
//        mvc.perform(get("/api/admin/context-test")
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.currentVendorId").value(loginResponse.vendorId().toString()));
//    }
//
//    @Test
//    @DisplayName("Test C: Staff Login (Wrong Silo Test)")
//    void testStaffLoginToWrongSilo() throws Exception {
//        StaffLoginRequest wrongSiloRequest = new StaffLoginRequest(
//                VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_B_SLUG // <-- The wrong slug
//        );
//
//        mvc.perform(post("/api/auth/admin/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(wrongSiloRequest)))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.message").value("User is not authorized for this vendor admin portal."));
//    }
//
//    @Test
//    @DisplayName("Test: Staff Login (Wrong Password Test)")
//    void testStaffLoginWithWrongPassword() throws Exception {
//        StaffLoginRequest wrongPassRequest = new StaffLoginRequest(
//                VENDOR_A_EMAIL, "WRONG-PASSWORD", VENDOR_A_SLUG
//        );
//
//        mvc.perform(post("/api/auth/admin/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(wrongPassRequest)))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.message").value("Invalid email or password."));
//    }
//
//    @Test
//    @DisplayName("Test D: The 'Air-Gap' (Use Client Token on Admin API)")
//    void testAirGap_ClientTokenOnAdminEndpoint() throws Exception {
//
//        // --- 1. Get a Client Token for Vendor B ---
//        String clientToken = getClientToken(
//                "test@customer.com", "password-C-789", VENDOR_B_SLUG
//        );
//
//        // --- 2. The "Attack": Use the Client Token on the Admin Endpoint ---
//        mvc.perform(get("/api/admin/context-test")
//                        .header("Authorization", "Bearer " + clientToken)) // <-- Using TOKEN_CUSTOMER
//                .andExpect(status().isUnauthorized()) // This is our corrected 401
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Authentication failed."));
//    }
//}