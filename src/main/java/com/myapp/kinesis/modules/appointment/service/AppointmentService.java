package com.myapp.kinesis.modules.appointment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.common.enums.AppointmentStatus;
import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.common.exceptions.TenantAccessDeniedException;
import com.myapp.kinesis.modules.appointment.dto.AvailabilityDto;
import com.myapp.kinesis.modules.appointment.dto.BookingDto;
import com.myapp.kinesis.modules.appointment.entity.AppointmentEntity;
import com.myapp.kinesis.modules.appointment.entity.AppointmentResourceEntity;
import com.myapp.kinesis.modules.appointment.repository.AppointmentRepository;
import com.myapp.kinesis.modules.appointment.repository.AppointmentResourceRepository;
import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import com.myapp.kinesis.modules.customer.service.ClientService;
import com.myapp.kinesis.modules.resource.entity.ResourceEntity;
import com.myapp.kinesis.modules.resource.repository.ResourceAvailabilityRepository;
import com.myapp.kinesis.modules.service.entity.ServiceEntity;
import com.myapp.kinesis.modules.service.repository.ServiceRepository;
import com.myapp.kinesis.modules.service.repository.ServiceResourceRepository;
import com.myapp.kinesis.modules.vendor.dto.BusinessHoursConfig;
import com.myapp.kinesis.modules.vendor.entity.VendorEntity;
import com.myapp.kinesis.modules.vendor.repository.VendorRepository;
import com.myapp.kinesis.modules.vendor.service.VendorService;
import com.myapp.kinesis.tenant.TenantContext;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * v4 (MVP Complete):
 * - Added resource availability checking
 * - Added business hours from vendor settings
 * - Added buffer time logic
 * - Added waitlist promotion
 */
@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AppointmentRepository appointmentRepository;
    private final AppointmentResourceRepository apptResourceRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceResourceRepository serviceResourceRepository;
    private final VendorRepository vendorRepository;
    private final ResourceAvailabilityRepository resourceAvailabilityRepository;
    private final ClientService clientService;
    private final VendorService vendorService;
    private final TenantContext tenantContext;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              AppointmentResourceRepository apptResourceRepository,
                              ServiceRepository serviceRepository,
                              ServiceResourceRepository serviceResourceRepository,
                              VendorRepository vendorRepository,
                              ResourceAvailabilityRepository resourceAvailabilityRepository,
                              ClientService clientService,
                              VendorService vendorService,
                              TenantContext tenantContext) {
        this.appointmentRepository = appointmentRepository;
        this.apptResourceRepository = apptResourceRepository;
        this.serviceRepository = serviceRepository;
        this.serviceResourceRepository = serviceResourceRepository;
        this.vendorRepository = vendorRepository;
        this.resourceAvailabilityRepository = resourceAvailabilityRepository;
        this.clientService = clientService;
        this.vendorService = vendorService;
        this.tenantContext = tenantContext;
    }

    // --- PUBLIC (GUEST) METHODS ---

    @Transactional(readOnly = true)
    public AvailabilityDto.AvailabilityResponse getAvailability(String vendorSlug, AvailabilityDto.AvailabilityRequest request) {

        VendorEntity vendor = vendorRepository.findBySlug(vendorSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "slug", vendorSlug));

        ServiceEntity service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.serviceId()));

        // Get business hours from vendor settings
        BusinessHoursConfig businessHours = vendorService.getBusinessHours(vendor.getId());

        LocalDate date = request.date();
        String dayOfWeek = date.getDayOfWeek().name().substring(0, 3); // "MON", "TUE", etc.

        BusinessHoursConfig.DayHours dayHours = businessHours.getBusinessHours().get(dayOfWeek);

        if (dayHours == null || dayHours.isClosed()) {
            // Vendor is closed on this day
            return new AvailabilityDto.AvailabilityResponse(List.of());
        }

        ZoneId vendorZone = ZoneId.of(businessHours.getTimezone());
        ZonedDateTime dayStart = date.atTime(dayHours.getOpen()).atZone(vendorZone);
        ZonedDateTime dayEnd = date.atTime(dayHours.getClose()).atZone(vendorZone);

        OffsetDateTime dayStartOffset = dayStart.toOffsetDateTime();
        OffsetDateTime dayEndOffset = dayEnd.toOffsetDateTime();

        int slotIncrement = businessHours.getSlotIncrement();
        long duration = service.getDurationMinutes();
        int capacity = getCapacityFromMetadata(service.getMetadata());
        List<UUID> requiredResourceIds = getRequiredResourceIds(service.getId());

        // Get buffer times
        int bufferBefore = getBufferFromMetadata(service.getMetadata(), "buffer_before");
        int bufferAfter = getBufferFromMetadata(service.getMetadata(), "buffer_after");

        List<AvailabilityDto.Slot> availableSlots = new ArrayList<>();
        OffsetDateTime currentSlotStart = dayStartOffset;

        while (currentSlotStart.isBefore(dayEndOffset)) {
            OffsetDateTime currentSlotEnd = currentSlotStart.plusMinutes(duration);

            if (currentSlotEnd.isAfter(dayEndOffset)) {
                break;
            }

            // Include buffer in availability check
            OffsetDateTime bufferStart = currentSlotStart.minusMinutes(bufferBefore);
            OffsetDateTime bufferEnd = currentSlotEnd.plusMinutes(bufferAfter);

            boolean isAvailable = isSlotAvailable(
                    vendor.getId(), service.getId(),
                    bufferStart, bufferEnd,
                    capacity, requiredResourceIds
            );

            if (isAvailable) {
                availableSlots.add(new AvailabilityDto.Slot(currentSlotStart, currentSlotEnd));
            }

            currentSlotStart = currentSlotStart.plusMinutes(slotIncrement);
        }

        return new AvailabilityDto.AvailabilityResponse(availableSlots);
    }

    @Transactional
    public BookingDto.BookingResponse createGuestBooking(String vendorSlug, BookingDto.BookingRequest request) {

        VendorEntity vendor = vendorRepository.findBySlug(vendorSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "slug", vendorSlug));
        ServiceEntity service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.serviceId()));

        ClientEntity client = clientService.findOrCreateGuestClient(
                vendor.getId(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.phone()
        );

        OffsetDateTime startTime = request.startTime();
        OffsetDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());
        int capacity = getCapacityFromMetadata(service.getMetadata());
        List<UUID> requiredResourceIds = getRequiredResourceIds(service.getId());

        // Get buffer times
        int bufferBefore = getBufferFromMetadata(service.getMetadata(), "buffer_before");
        int bufferAfter = getBufferFromMetadata(service.getMetadata(), "buffer_after");

        OffsetDateTime bufferStart = startTime.minusMinutes(bufferBefore);
        OffsetDateTime bufferEnd = endTime.plusMinutes(bufferAfter);

        if (!isSlotAvailable(vendor.getId(), service.getId(), bufferStart, bufferEnd, capacity, requiredResourceIds)) {
            throw new IllegalStateException("This time slot is no longer available. Please select another.");
        }

        AppointmentEntity appt = new AppointmentEntity();
        appt.setVendor(vendor);
        appt.setClient(client);
        appt.setService(service);
        appt.setStartTime(startTime);
        appt.setEndTime(endTime);
        appt.setStatus(AppointmentStatus.PENDING);
        appt.setPriceCharged(service.getPrice());
        appt.setBookingContactName(request.firstName() + " " + (request.lastName() != null ? request.lastName() : ""));
        appt.setBookingContactEmail(request.email());
        appt.setBookingContactPhone(request.phone());

        if (request.metadata() != null) {
            appt.setMetadata(request.metadata().toString());
        }

        AppointmentEntity savedAppt = appointmentRepository.save(appt);

        for (UUID resourceId : requiredResourceIds) {
            ResourceEntity resource = new ResourceEntity();
            resource.setId(resourceId);
            AppointmentResourceEntity link = new AppointmentResourceEntity(savedAppt, resource);
            apptResourceRepository.save(link);
        }

        logger.info("New booking created: {}", savedAppt.getId());

        return BookingDto.BookingResponse.fromEntity(savedAppt);
    }

    // --- ADMIN (PWA) METHODS ---

    @Transactional(readOnly = true)
    public List<AppointmentEntity> getAppointmentsForCurrentVendor() {
        UUID vendorId = tenantContext.getVendorId();
        return appointmentRepository.findAllByVendorId(vendorId);
    }

    @Transactional
    public AppointmentEntity confirmAppointment(UUID appointmentId) {
        UUID vendorId = tenantContext.getVendorId();

        AppointmentEntity appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!appt.getVendor().getId().equals(vendorId)) {
            throw new TenantAccessDeniedException("Access Denied: This appointment does not belong to your vendor account.");
        }

        appt.setStatus(AppointmentStatus.CONFIRMED);
        logger.info("Appointment {} confirmed by vendor {}", appointmentId, vendorId);

        return appointmentRepository.save(appt);
    }

    @Transactional
    public AppointmentEntity cancelAppointment(UUID appointmentId) {
        UUID vendorId = tenantContext.getVendorId();

        AppointmentEntity appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!appt.getVendor().getId().equals(vendorId)) {
            throw new TenantAccessDeniedException("Access Denied: This appointment does not belong to your vendor account.");
        }

        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);

        logger.info("Appointment {} cancelled by vendor {}", appointmentId, vendorId);

        // Check if anyone is waitlisted for this slot
        promoteFromWaitlist(appt.getService().getId(), appt.getStartTime());

        return appt;
    }

    // --- HELPER METHODS ---

    private boolean isSlotAvailable(UUID vendorId, UUID serviceId,
                                    OffsetDateTime startTime, OffsetDateTime endTime,
                                    int capacity, List<UUID> requiredResourceIds) {

        // Check 1: Resource conflicts
        if (!requiredResourceIds.isEmpty()) {
            List<AppointmentEntity> conflictingAppointments =
                    appointmentRepository.findConflictingAppointments(vendorId, startTime, endTime, requiredResourceIds);

            if (!conflictingAppointments.isEmpty()) {
                logger.debug("Slot unavailable: Resource conflict found for vendorId {}", vendorId);
                return false;
            }

            // Check 2: Resource availability (vacations, breaks)
            for (UUID resourceId : requiredResourceIds) {
                boolean isUnavailable = resourceAvailabilityRepository
                        .existsByResourceIdAndTimeRange(resourceId, startTime, endTime);

                if (isUnavailable) {
                    logger.debug("Slot unavailable: Resource {} is on vacation/break", resourceId);
                    return false;
                }
            }
        }

        // Check 3: Group capacity
        if (capacity > 0) {
            int existingBookings = appointmentRepository.countByServiceIdAndStartTimeAndStatusNot(
                    serviceId, startTime, AppointmentStatus.CANCELLED
            );

            if (existingBookings >= capacity) {
                logger.debug("Slot unavailable: Service capacity ({}) met", capacity);
                return false;
            }
        }

        return true;
    }

    private void promoteFromWaitlist(UUID serviceId, OffsetDateTime startTime) {
        AppointmentEntity waitlisted = appointmentRepository.findFirstWaitlisted(serviceId, startTime);

        if (waitlisted != null) {
            waitlisted.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(waitlisted);

            logger.info("Promoted appointment {} from waitlist", waitlisted.getId());
            // TODO: Send notification to client
        }
    }

    @SneakyThrows
    private int getCapacityFromMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return 1;
        }
        JsonNode node = objectMapper.readTree(metadata);
        if (node.has("capacity")) {
            return node.get("capacity").asInt(1);
        }
        return 1;
    }

    @SneakyThrows
    private int getBufferFromMetadata(String metadata, String field) {
        if (metadata == null || metadata.isBlank()) {
            return 0;
        }
        JsonNode node = objectMapper.readTree(metadata);
        if (node.has(field)) {
            return node.get(field).asInt(0);
        }
        return 0;
    }

    private List<UUID> getRequiredResourceIds(UUID serviceId) {
        return serviceResourceRepository.findAllByServiceId(serviceId).stream()
                .map(link -> link.getResource().getId())
                .collect(Collectors.toList());
    }
}