package com.myapp.kinesis.common.enums;

/**
 * Defines the "Time Model" for a service (the "When").
 * This maps to the "service_booking_model" ENUM in PostgreSQL.
 */
public enum ServiceBookingModel {
    /**
     * A fixed, future time slot (e.g., 10:30 AM).
     * Used by: Clinics, Spas, Salons (for priority).
     */
    SCHEDULED,

    /**
     * A "first-come, first-served" waitlist for ASAP service.
     * Used by: Barbers, Walk-in Clinics.
     */
    QUEUE,

    /**
     * A block of time with a start and end (e.g., 3 hours).
     * Used by: Tool Rentals, Studio Rentals.
     */
    RENTAL,

    /**
     * An intake form for a long-term project.
     * Used by: Lawyers, Builders, Contractors.
     */
    PROJECT
}
