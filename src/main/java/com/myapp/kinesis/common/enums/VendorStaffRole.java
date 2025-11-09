package com.myapp.kinesis.common.enums;

/**
 * Defines the sub-role for a user *within* a vendor
 * This maps to the "vendor_staff_role" ENUM in PostgreSQL.
 */
public enum VendorStaffRole {
    /**
     * Full admin rights for a specific vendor. Can manage staff,
     * settings, and billing.
     */
    VENDOR_OWNER,

    /**
     * Regular employee. Can manage appointments and clients,
     * but cannot change vendor settings or manage other staff.
     */
    STAFF
}