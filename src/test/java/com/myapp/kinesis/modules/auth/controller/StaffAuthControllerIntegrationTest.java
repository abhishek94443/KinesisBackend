package com.myapp.kinesis.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.modules.auth.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test for the "Air-Gap" Authentication and Tenancy Flow.
 *
 * @SpringBootTest: Loads the entire Spring application context.
 * @AutoConfigureMockMvc: Injects a MockMvc bean to simulate HTTP requests.
 * @ActiveProfiles("test"): Forces the app to use 'application-test.properties'.
 * @Transactional: (CRITICAL) Rolls back all database changes after each test,
 * so every test runs on a clean, empty database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test") // This is crucial! It loads application-test.properties
@Transactional // This is crucial! It rolls back the database after each test
public class StaffAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mvc; // The tool for simulating API calls

    @Autowired
    private ObjectMapper objectMapper; // For converting DTOs to JSON

    // Define test constants for clarity
    private static final String VENDOR_A_SLUG = "test-clinic-a";
    private static final String VENDOR_A_EMAIL = "owner@clinic-a.com";
    private static final String VENDOR_A_PASS = "password-A-123";

    private static final String VENDOR_B_SLUG = "test-gym-b";
    private static final String VENDOR_B_EMAIL = "owner@gym-b.com";
    private static final String VENDOR_B_PASS = "password-B-456";

    /**
     * Runs before each @Test method.
     * We use this to set up our two vendors (Silos A and B)
     * so our tests have a clean environment.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Test Suite A (Part 1): Create Vendor A
        VendorRegistrationRequest vendorA = new VendorRegistrationRequest(
                "Test Clinic A", VENDOR_A_SLUG, VENDOR_A_EMAIL, VENDOR_A_PASS
        );
        mvc.perform(post("/api/auth/admin/register-vendor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorA)))
                .andExpect(status().isCreated());

        // Test Suite A (Part 2): Create Vendor B
        VendorRegistrationRequest vendorB = new VendorRegistrationRequest(
                "Test Gym B", VENDOR_B_SLUG, VENDOR_B_EMAIL, VENDOR_B_PASS
        );
        mvc.perform(post("/api/auth/admin/register-vendor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorB)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Test B: Staff Login (Happy Path) & TenantContext")
    void testStaffLoginAndContext() throws Exception {
        // --- Test B (Part 1): Login to Vendor A ---
        StaffLoginRequest loginRequest = new StaffLoginRequest(
                VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_A_SLUG
        );

        MvcResult loginResult = mvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // We expect 200 OK
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_VENDOR_OWNER"))
                .andReturn();

        // Extract the token for the next test
        String responseBody = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);
        String token = loginResponse.token();

        // --- Test B (Part 2): Test the "Backpack" (TenantContext) ---
        // This test requires the StaffAdminController (and its /context-test endpoint) to exist.
        // We will add that file next. For now, we are testing the login.

        // mvc.perform(get("/api/admin/context-test")
        //         .header("Authorization", "Bearer " + token))
        //     .andExpect(status().isOk())
        //     .andExpect(jsonPath("$.success").value(true))
        //     .andExpect(jsonPath("$.data.currentVendorId").value(loginResponse.vendorId().toString()));
    }

    @Test
    @DisplayName("Test C: Staff Login (Wrong Silo Test)")
    void testStaffLoginToWrongSilo() throws Exception {
        // Try to log in with Vendor A's credentials, but at Vendor B's slug
        StaffLoginRequest wrongSiloRequest = new StaffLoginRequest(
                VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_B_SLUG // <-- The wrong slug
        );

        mvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongSiloRequest)))
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User is not authorized for this vendor admin portal."));
    }

    @Test
    @DisplayName("Test: Staff Login (Wrong Password Test)")
    void testStaffLoginWithWrongPassword() throws Exception {
        StaffLoginRequest wrongPassRequest = new StaffLoginRequest(
                VENDOR_A_EMAIL, "WRONG-PASSWORD", VENDOR_A_SLUG
        );

        mvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassRequest)))
                .andExpect(status().isUnauthorized()) // 4Must be 401
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("Test D: The 'Air-Gap' (Use Client Token on Admin API)")
    void testAirGap_ClientTokenOnAdminEndpoint() throws Exception {

        // --- 1. Create a Customer at Vendor B ---
        ClientRegistrationRequest clientReg = new ClientRegistrationRequest(
                "test@customer.com", "password-C-789", "Test", "Customer", "555555", VENDOR_B_SLUG
        );
        mvc.perform(post("/api/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientReg)))
                .andExpect(status().isCreated());

        // --- 2. Log in as that Customer to get a Client Token ---
        ClientLoginRequest clientLogin = new ClientLoginRequest(
                "test@customer.com", "password-C-789", VENDOR_B_SLUG
        );
        MvcResult loginResult = mvc.perform(post("/api/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String clientToken = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class
        ).token();

        // --- 3. The "Attack": Use the Client Token on the Admin Endpoint ---
        // We will try to access the /api/admin/context-test endpoint,
        // which we will create in the next step.

        // mvc.perform(get("/api/admin/context-test")
        //         .header("Authorization", "Bearer " + clientToken)) // <-- Using TOKEN_CUSTOMER
        //     .andExpect(status().isUnauthorized()) // <-- This is the correct 401
        //     .andExpect(jsonPath("$.success").value(false))
        //     .andExpect(jsonPath("$.message").value("Authentication failed."));
    }
}