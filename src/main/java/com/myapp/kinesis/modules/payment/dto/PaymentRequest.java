package com.myapp.kinesis.modules.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull(message = "Appointment ID is required")
        UUID appointmentId,

        @NotNull(message = "Amount is required")
        @Min(value = 0, message = "Amount cannot be negative")
        BigDecimal amount,

        @NotBlank(message = "Payment method is required")
        String paymentMethod, // "RAZORPAY", "CASH"

        String paymentType // "FULL", "DEPOSIT"
) {
}