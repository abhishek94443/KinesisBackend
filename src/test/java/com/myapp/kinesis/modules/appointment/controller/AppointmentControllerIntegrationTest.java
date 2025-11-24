//package com.myapp.kinesis.modules.appointment.controller;
//
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.myapp.kinesis.BaseIntegrationTest;
//import com.myapp.kinesis.common.enums.ResourceType;
//import com.myapp.kinesis.modules.appointment.dto.AvailabilityDto;
//import com.myapp.kinesis.modules.appointment.dto.BookingDto;
//import com.myapp.kinesis.modules.auth.dto.LoginResponse;
//import com.myapp.kinesis.modules.resource.dto.ResourceResponse;
//import com.myapp.kinesis.modules.service.dto.ServiceResponseDto;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.time.LocalDate;
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.UUID;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//public class AppointmentControllerIntegrationTest extends BaseIntegrationTest {
//
//    private String tokenA;
//    private UUID vendorAId;
//    private ServiceResponseDto clinicService;
//
//    // We'll use a fixed date for predictable tests
//    private final LocalDate testDate = LocalDate.now().plusDays(1);
//    private final OffsetDateTime testTimeSlot = testDate.atTime(10, 0).atOffset(ZoneOffset.UTC);
//
//    // --- THIS IS THE FIX for Bug 1 ---
//    // Create a formatter that *exactly* matches our DTO's @JsonFormat
//    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME_WITH_SECONDS =
//            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
//
//
//    @BeforeEach
//    void setUpAppointments() throws Exception {
//        LoginResponse loginA = loginAsStaff(VENDOR_A_EMAIL, VENDOR_A_PASS, VENDOR_A_SLUG);
//        tokenA = loginA.token();
//        vendorAId = loginA.vendorId();
//
//        ResourceResponse doctor = createResource(tokenA, "Dr. Smith", ResourceType.STAFF);
//        ResourceResponse room = createResource(tokenA, "Exam Room 1", ResourceType.ROOM);
//
//        ObjectNode metadata = objectMapper.createObjectNode();
//        metadata.put("booking_model", "SCHEDULED");
//        metadata.put("capacity", 1);
//
//        clinicService = createService(
//                tokenA,
//                "Dental Cleaning",
//                60,
//                List.of(doctor.id(), room.id()),
//                metadata
//        );
//    }
//
//    @Test
//    @DisplayName("Test 1: Public Availability (Happy Path)")
//    void testGetAvailability() throws Exception {
//        AvailabilityDto.AvailabilityRequest availRequest = new AvailabilityDto.AvailabilityRequest(
//                clinicService.id(),
//                testDate
//        );
//
//        // --- THIS IS THE FIX for Bug 1 ---
//        // We format our expected time to match the DTO's output
//        String expectedStartTime = testDate.atTime(9, 0).atOffset(ZoneOffset.UTC)
//                .format(ISO_OFFSET_DATE_TIME_WITH_SECONDS);
//
//        mvc.perform(post("/api/public/availability/" + VENDOR_A_SLUG)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(availRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.availableSlots").isArray())
//                // We now assert against the correctly formatted string
//                .andExpect(jsonPath("$.data.availableSlots[0].startTime").value(expectedStartTime));
//    }
//
//    @Test
//    @DisplayName("Test 2: Public Guest Booking (Happy Path) & Admin Confirm")
//    void testGuestBooking_Success_And_AdminConfirm() throws Exception {
//        BookingDto.BookingRequest bookingRequest = new BookingDto.BookingRequest(
//                clinicService.id(),
//                testTimeSlot, // 10:00 AM
//                "Test", "Patient", "test@patient.com", "555-1234",
//                objectMapper.createObjectNode().put("reason", "Annual Checkup")
//        );
//
//        // 1. Perform the Guest Booking
//        MvcResult result = mvc.perform(post("/api/public/book/" + VENDOR_A_SLUG)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(bookingRequest)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.appointmentId").exists())
//                .andExpect(jsonPath("$.data.status").value("PENDING"))
//                .andReturn();
//
//        // Extract the new appointment ID
//        String jsonResponse = result.getResponse().getContentAsString();
//        UUID appointmentId = UUID.fromString(objectMapper.readTree(jsonResponse).get("data").get("appointmentId").asText());
//
//        // 2. Admin Verifies: Log in as staff and confirm the booking
//        mvc.perform(put("/api/admin/appointments/" + appointmentId + "/confirm")
//                        .header("Authorization", "Bearer " + tokenA))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
//    }
//
//    @Test
//    @DisplayName("Test 3: Booking Conflict (Double Booking)")
//    void testGuestBooking_FailsOnConflict() throws Exception {
//        BookingDto.BookingRequest bookingRequest1 = new BookingDto.BookingRequest(
//                clinicService.id(),
//                testTimeSlot, // 10:00 AM
//                "Patient One", "One", "p1@email.com", "111", null
//        );
//
//        BookingDto.BookingRequest bookingRequest2 = new BookingDto.BookingRequest(
//                clinicService.id(),
//                testTimeSlot, // 10:00 AM (Same time!)
//                "Patient Two", "Two", "p2@email.com", "222", null
//        );
//
//        // 1. Book the 10:00 AM slot
//        mvc.perform(post("/api/public/book/" + VENDOR_A_SLUG)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(bookingRequest1)))
//                .andExpect(status().isCreated());
//
//        // 2. Try to book the *exact same* 10:00 AM slot
//        mvc.perform(post("/api/public/book/" + VENDOR_A_SLUG)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(bookingRequest2)))
//                .andExpect(status().isBadRequest()) // We fixed this to expect 400
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("This time slot is no longer available. Please select another."));
//    }
//}