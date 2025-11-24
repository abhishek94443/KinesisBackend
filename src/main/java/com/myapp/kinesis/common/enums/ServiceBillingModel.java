package com.myapp.kinesis.common.enums;

/**
 * Defines the "Billing Model" for a service (the "How").
 * This maps to the "service_billing_model" ENUM in PostgreSQL.
 */
public enum ServiceBillingModel {
    /**
     * Standard transactional payment.
     * The customer pays the 'services.price'.
     */
    PAY_PER_USE,

    /**
     * A recurring fee (e.g., monthly) makes this service free.
     * Logic will check 'clients.metadata.subscription_status'.
     */
    SUBSCRIPTION,

    /**
     * A pre-paid pack of 10 appointments.
     * Logic will check 'clients.metadata.credits_remaining'.
     */
    CREDIT_PACK
}