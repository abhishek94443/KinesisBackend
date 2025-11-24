package com.myapp.kinesis.modules.payment.dto;

import com.myapp.kinesis.modules.payment.entity.PaymentEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID appointmentId,
        BigDecimal amount,
        String currency,
        String paymentType,
        String paymentMethod,
        String paymentStatus,
        String transactionId,
        OffsetDateTime createdAt
) {
    public static PaymentResponse fromEntity(PaymentEntity entity) {
        return new PaymentResponse(
                entity.getId(),
                entity.getAppointment().getId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getPaymentType(),
                entity.getPaymentMethod(),
                entity.getPaymentStatus(),
                entity.getTransactionId(),
                entity.getCreatedAt()
        );
    }
}