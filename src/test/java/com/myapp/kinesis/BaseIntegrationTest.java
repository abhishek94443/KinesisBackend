//package com.myapp.kinesis;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.myapp.kinesis.common.enums.ResourceType;
//import com.myapp.kinesis.modules.auth.dto.*;
//import com.myapp.kinesis.modules.resource.dto.ResourceRequest;
//import com.myapp.kinesis.modules.resource.dto.ResourceResponse;
//import com.myapp.kinesis.modules.service.dto.ServiceRequestDto;
//import com.myapp.kinesis.modules.service.dto.ServiceResponseDto;
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.UUID;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * v3 (Corrected): Added all missing DTO import statements.
// * This is the shared, abstract base class for all integration tests.
// */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@Transactional
//public abstract class BaseIntegrationTest {
//
//    @Autowired
//    protected MockMvc mvc;
//
//    @Autowired
//    protected ObjectMapper objectMapper;
//
//    // --- Constants ---
//    protected static final String VENDOR_A_SLUG = "test-clinic-a";
//    protected static final String VENDOR_A_EMAIL = "owner@clinic-a.com";
//    protected static final String VENDOR_A_PASS = "password-A-123";
//
//    protected static final String VENDOR_B_SLUG = "test-gym-b";
//    protected static final String VENDOR_B_EMAIL = "owner@gym-b.com";
//    protected static final String VENDOR_B_PASS = "password-B-456";
//
//    /**
//     * Runs before each @Test method.
//     * Ensures our two vendors are registered.
//     */
//    @BeforeEach
//    void setUpBase() throws Exception {
//        VendorRegistrationRequest vendorA = new VendorRegistrationRequest(
//                "Test Clinic A", VENDOR_A_SLUG, "Admin", "User", VENDOR_A_EMAIL, VENDOR_A_PASS
//        );
//        mvc.perform(post("/api/auth/admin/register-vendor")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(vendorA)))
//                .andExpect(status().isCreated());
//
//        VendorRegistrationRequest vendorB = new VendorRegistrationRequest(
//                "Test Gym B", VENDOR_B_SLUG, "Admin", "User", VENDOR_B_EMAIL, VENDOR_B_PASS
//        );
//        mvc.perform(post("/api/auth/admin/register-vendor")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(vendorB)))
//                .andExpect(status().isCreated());
//    }
//
//    // --- Helper Methods ---
//
//    /**
//     * Performs a Staff login and returns the full LoginResponse (with token).
//     */
//    protected LoginResponse loginAsStaff(String email, String password, String slug) throws Exception {
//        StaffLoginRequest loginRequest = new StaffLoginRequest(email, password, slug);
//
//        MvcResult loginResult = mvc.perform(post("/api/auth/admin/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        return objectMapper.readValue(
//                loginResult.getResponse().getContentAsString(), LoginResponse.class
//        );
//    }
//
//    /**
//     * Helper: Registers AND logs in a new customer, returning their token.
//     */
//    protected String getClientToken(String email, String password, String slug) throws Exception {
//        // 1. Register the customer
//        ClientRegistrationRequest clientReg = new ClientRegistrationRequest(
//                email, password, "Test", "Client", "555555", slug
//        );
//        mvc.perform(post("/api/auth/customer/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientReg)))
//                .andExpect(status().isCreated());
//
//        // 2. Log in as the customer
//        ClientLoginRequest clientLogin = new ClientLoginRequest(email, password, slug);
//        MvcResult loginResult = mvc.perform(post("/api/auth/customer/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(clientLogin)))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        return objectMapper.readValue(
//                loginResult.getResponse().getContentAsString(), LoginResponse.class
//        ).token();
//    }
//
//    /**
//     * Helper: Creates a resource for a vendor and returns the DTO.
//     */
//    protected ResourceResponse createResource(String token, String name, ResourceType type) throws Exception {
//        ResourceRequest newResource = new ResourceRequest(name, type, null);
//
//        MvcResult result = mvc.perform(post("/api/admin/resources")
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(newResource)))
//                .andExpect(status().isCreated())
//                .andReturn();
//
//        String jsonResponse = result.getResponse().getContentAsString();
//        return objectMapper.convertValue(
//                objectMapper.readTree(jsonResponse).get("data"),
//                ResourceResponse.class
//        );
//    }
//
//    /**
//     * Helper: Creates a service for a vendor and returns the DTO.
//     */
//    protected ServiceResponseDto createService(String token, String name, int duration, List<UUID> resourceIds, JsonNode metadata) throws Exception {
//        ServiceRequestDto newService = new ServiceRequestDto(
//                name,
//                "Test description",
//                duration,
//                BigDecimal.TEN,
//                true,
//                metadata,
//                resourceIds
//        );
//
//        MvcResult result = mvc.perform(post("/api/admin/services")
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(newService)))
//                .andExpect(status().isCreated())
//                .andReturn();
//
//        String jsonResponse = result.getResponse().getContentAsString();
//        return objectMapper.convertValue(
//                objectMapper.readTree(jsonResponse).get("data"),
//                ServiceResponseDto.class
//        );
//    }
//}