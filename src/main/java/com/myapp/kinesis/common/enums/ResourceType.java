package com.myapp.kinesis.common.enums;

/**
 * Represents the generic "behavioral type" of a bookable resource.
 * This maps to the "resource_type_enum" in PostgreSQL.
 * <p>
 * This abstraction is critical for our "Universal" platform.
 */
public enum ResourceType {

    /**
     * A human resource that provides a service.
     * (e.g., Doctor, Lawyer, Trainer, Stylist, Teacher).
     * This implies the resource has a human schedule.
     */
    STAFF,

    /**
     * A physical location or space that can be booked.
     * (e.g., Exam Room 1, Main Studio, Conference Room, Court 3).
     * This implies the resource has a 1-to-1 booking limit.
     */
    ROOM,

    /**
     * A non-human, tangible asset that can be booked.
     * (e.g., Spin Bike #5, Laser Machine, Excavator #1).
     * This implies a pool of similar items may exist.
     */
    EQUIPMENT
}