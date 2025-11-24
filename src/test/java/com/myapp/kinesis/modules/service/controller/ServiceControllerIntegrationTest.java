//package com.myapp.kinesis.modules.service.controller;
//
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.myapp.kinesis.BaseIntegrationTest;
//import com.myapp.kinesis.common.enums.ResourceType;
//import com.myapp.kinesis.modules.auth.dto.LoginResponse;
//import com.myapp.kinesis.modules.resource.dto.ResourceResponse;
//import com.myapp.kinesis.modules.service.dto.ServiceRequestDto;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//public class ServiceControllerIntegrationTest extends BaseIntegrationTest {
//
//    private String tokenA;
//    private String tokenB;
//    private ResourceResponse resourceA_Doctor;
//    private ResourceResponse resourceA_Room;
//    private ResourceResponse resourceB_Trainer;
//
//    @BeforeEach
//    void setUpServices() throws Exception {
//        LoginResponse loginA = loginAsStaff(VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_A_SLUG);
//        LoginResponse loginB = loginAsStaff(VENDOR_B_EMAIL, VENDOR_B_PASS, VENDOR_B_SLUG);
//        tokenA = loginA.token();
//        tokenB = loginB.token();
//
//        resourceA_Doctor = createResource(tokenA, "Dr. Smith (Clinic A)", ResourceType.STAFF);
//        resourceA_Room = createResource(tokenA, "Exam Room 1 (Clinic A)", ResourceType.ROOM);
//        resourceB_Trainer = createResource(tokenB, "Trainer Bob (Gym B)", ResourceType.STAFF);
//    }
//
//    @Test
//    @DisplayName("Test 1: Create Service (Happy Path) & Get Public Service")
//    void testCreateAndGetPublicService() throws Exception {
//
//        ObjectNode metadata = objectMapper.createObjectNode();
//        metadata.put("booking_model", "SCHEDULED");
//        metadata.put("capacity", 1);
//
//        ServiceRequestDto newService = new ServiceRequestDto(
//                "Dental Cleaning",
//                "A 60-minute cleaning.",
//                60,
//                new BigDecimal("150.00"),
//                true,
//                metadata,
//                List.of(resourceA_Doctor.id(), resourceA_Room.id())
//        );
//
//        mvc.perform(post("/api/admin/services")
//                        .header("Authorization", "Bearer " + tokenA)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(newService)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.name").value("Dental Cleaning"))
//                .andExpect(jsonPath("$.data.resources", hasSize(2)))
//                .andExpect(jsonPath("$.data.resources[0].name", is("Dr. Smith (Clinic A)")));
//
//        mvc.perform(get("/api/public/services/" + VENDOR_A_SLUG))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].name").value("Dental Cleaning"));
//    }
//
//    @Test
//    @DisplayName("Test 2: Create Service Fails (Tenant Isolation)")
//    void testCreateService_FailsTenantIsolation() throws Exception {
//
//        ServiceRequestDto badRequest = new ServiceRequestDto(
//                "Stolen Resource Service",
//                null,
//                30,
//                BigDecimal.TEN,
//                true,
//                null,
//                List.of(resourceB_Trainer.id()) // <-- Using Vendor B's resource ID
//        );
//
//        mvc.perform(post("/api/admin/services")
//                        .header("Authorization", "Bearer " + tokenA) // <-- As Vendor A
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(badRequest)))
//                .andExpect(status().isUnauthorized()) // 401
//                .andExpect(jsonPath("$.success").value(false))
//
//                // --- THIS IS THE FIX ---
//                // We now check for the *exact* message our service throws.
//                .andExpect(jsonPath("$.message").value("Cannot link resource from another vendor."));
//    }
//}