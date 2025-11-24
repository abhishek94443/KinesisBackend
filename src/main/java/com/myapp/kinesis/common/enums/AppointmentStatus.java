package com.myapp.kinesis.common.enums;

/**
 * Defines the lifecycle status of an Appointment.
 * Maps to the "appointment_status_enum" in PostgreSQL.
 */
public enum AppointmentStatus {
    /**
     * Booked by a guest, needs staff confirmation.
     */
    PENDING,

    /**
     * Confirmed. The slot is locked.
     */
    CONFIRMED,

    /**
     * Cancelled by either the customer or the staff.
     */
    CANCELLED,

    /**
     * The appointment has passed and was successfully fulfilled.
     */
    COMPLETED,

    /**
     * The customer (for a SCHEDULED booking) never arrived.
     */
    NO_SHOW,

    /**
     * The customer (for a QUEUE booking) was not present when called.
     */
    SKIPPED
}