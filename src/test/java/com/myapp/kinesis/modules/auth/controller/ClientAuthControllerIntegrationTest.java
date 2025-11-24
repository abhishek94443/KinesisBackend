//package com.myapp.kinesis.modules.auth.controller;
//
//import com.myapp.kinesis.BaseIntegrationTest;
//import com.myapp.kinesis.modules.auth.dto.ClientLoginRequest;
//import com.myapp.kinesis.modules.auth.dto.ClientRegistrationRequest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * Integration Test for the "Client" side of the "Air-Gap" auth.
// */
//public class ClientAuthControllerIntegrationTest extends BaseIntegrationTest {
//
//    // Note: The @BeforeEach from BaseIntegrationTest already runs
//    // and creates VENDOR_A and VENDOR_B for us.
//
//    @Test
//    @DisplayName("Test 1: Client Registration (Happy Path)")
//    void testRegisterClient_Success() throws Exception {
//        ClientRegistrationRequest clientReg = new ClientRegistrationRequest(
//                "test@customer.com", "password-C-789", "Test", "Customer", "555555", VENDOR_A_SLUG
//        );
//
//        mvc.perform(post("/api/auth/customer/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientReg)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    @Test
//    @DisplayName("Test 2: Client Registration Fails (Duplicate Email in same Vendor)")
//    void testRegisterClient_FailsDuplicateEmail() throws Exception {
//        // 1. Create the first client
//        ClientRegistrationRequest clientReg = new ClientRegistrationRequest(
//                "test@customer.com", "password-C-789", "Test", "Customer", "555555", VENDOR_A_SLUG
//        );
//        mvc.perform(post("/api/auth/customer/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientReg)))
//                .andExpect(status().isCreated());
//
//        // 2. Try to create the *same client* at the *same vendor*
//        mvc.perform(post("/api/auth/customer/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientReg)))
//                .andExpect(status().isBadRequest()) // 400
//                .andExpect(jsonPath("$.message").value("Error: Email is already in use at this location."));
//    }
//
//    @Test
//    @DisplayName("Test 3: Client Registration Succeeds (Duplicate Email in DIFFERENT Vendor)")
//    void testRegisterClient_SuccessDuplicateEmailDifferentVendor() throws Exception {
//        // 1. Create the client at Vendor A
//        ClientRegistrationRequest clientRegA = new ClientRegistrationRequest(
//                "test@customer.com", "password-C-789", "Test", "Customer", "555555", VENDOR_A_SLUG
//        );
//        mvc.perform(post("/api/auth/customer/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientRegA)))
//                .andExpect(status().isCreated());
//
//        // 2. Create the *same client* at Vendor B (This MUST succeed)
//        ClientRegistrationRequest clientRegB = new ClientRegistrationRequest(
//                "test@customer.com", "password-C-789", "Test", "Customer", "555555", VENDOR_B_SLUG
//        );
//        mvc.perform(post("/api/auth/customer/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientRegB)))
//                .andExpect(status().isCreated());
//    }
//
//    @Test
//    @DisplayName("Test 4: Client Login (Happy Path)")
//    void testClientLogin_Success() throws Exception {
//        // 1. Register the client
//        testRegisterClient_Success();
//
//        // 2. Log in
//        ClientLoginRequest clientLogin = new ClientLoginRequest(
//                "test@customer.com", "password-C-789", VENDOR_A_SLUG
//        );
//        mvc.perform(post("/api/auth/customer/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientLogin)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").exists())
//                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"));
//    }
//
//    @Test
//    @DisplayName("Test 5: Client Login Fails (Wrong Silo)")
//    void testClientLogin_WrongSilo() throws Exception {
//        // 1. Register the client at VENDOR A
//        testRegisterClient_Success();
//
//        // 2. Try to log in at VENDOR B
//        ClientLoginRequest wrongSiloLogin = new ClientLoginRequest(
//                "test@customer.com", "password-C-789", VENDOR_B_SLUG // <-- Wrong slug
//        );
//        mvc.perform(post("/api/auth/customer/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(wrongSiloLogin)))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.message").value("Invalid email or password."));
//    }
//}