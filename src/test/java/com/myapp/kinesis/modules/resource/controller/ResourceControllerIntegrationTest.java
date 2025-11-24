//package com.myapp.kinesis.modules.resource.controller;
//
//import com.myapp.kinesis.BaseIntegrationTest;
//import com.myapp.kinesis.common.enums.ResourceType;
//import com.myapp.kinesis.modules.auth.dto.LoginResponse;
//import com.myapp.kinesis.modules.resource.dto.ResourceRequest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * Integration Test for the Resource Module.
// * This class extends BaseIntegrationTest to get all the
// * setup (like vendor registration) and helper methods for free.
// */
//public class ResourceControllerIntegrationTest extends BaseIntegrationTest {
//
//    @Test
//    @DisplayName("Test 1: Create Resource (Happy Path)")
//    void testCreateResource() throws Exception {
//        // 1. Get a valid token for Vendor A
//        LoginResponse loginA = loginAsStaff(VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_A_SLUG);
//
//        // 2. Create the DTO for our new resource
//        ResourceRequest newResource = new ResourceRequest(
//                "Dr. Jane Smith",
//                ResourceType.STAFF,
//                objectMapper.createObjectNode().put("title", "Senior Dentist")
//        );
//
//        // 3. Perform the API call
//        mvc.perform(post("/api/admin/resources")
//                        .header("Authorization", "Bearer " + loginA.token())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(newResource)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.name").value("Dr. Jane Smith"))
//                .andExpect(jsonPath("$.data.type").value("STAFF"))
//                .andExpect(jsonPath("$.data.vendorId").value(loginA.vendorId().toString()))
//                .andExpect(jsonPath("$.data.metadata.title").value("Senior Dentist"));
//    }
//
//    @Test
//    @DisplayName("Test 2: Create Resource Fails (Validation Error)")
//    void testCreateResource_FailsValidation() throws Exception {
//        // 1. Get a valid token
//        String token = loginAsStaff(VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_A_SLUG).token();
//
//        // 2. Create a *bad* DTO (name is blank)
//        ResourceRequest badRequest = new ResourceRequest(
//                "", // <-- Blank name
//                ResourceType.STAFF,
//                null
//        );
//
//        // 3. Perform the API call
//        mvc.perform(post("/api/admin/resources")
//                        .header("Authorization", "Bearer " + token)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(badRequest)))
//                .andExpect(status().isBadRequest()) // Expect 400
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Validation Failed"))
//                .andExpect(jsonPath("$.data.name").value("Resource name is required (e.g., 'Dr. Smith', 'Room 1')"));
//    }
//
//    @Test
//    @DisplayName("Test 3: List Resources (Isolation Test)")
//    void testListResources_Isolation() throws Exception {
//        // 1. Get tokens for BOTH vendors
//        String tokenA = loginAsStaff(VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_A_SLUG).token();
//        String tokenB = loginAsStaff(VENDOR_B_EMAIL, VENDOR_B_PASS, VENDOR_B_SLUG).token();
//
//        // 2. Create "Resource A" for Vendor A (using our helper)
//        createResource(tokenA, "Dr. Smith (Clinic A)", ResourceType.STAFF);
//
//        // 3. Create "Resource B" for Vendor B (using our helper)
//        createResource(tokenB, "Spin Bike #1 (Gym B)", ResourceType.EQUIPMENT);
//
//        // 4. THE TEST: Log in as Vendor A and list resources
//        mvc.perform(get("/api/admin/resources")
//                        .header("Authorization", "Bearer " + tokenA)) // <-- Using Token A
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                // It should find EXACTLY 1 resource
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                // That resource should be "Dr. Smith (Clinic A)"
//                .andExpect(jsonPath("$.data[0].name", is("Dr. Smith (Clinic A)")));
//    }
//}