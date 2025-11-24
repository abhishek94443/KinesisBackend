package com.myapp.kinesis.modules.payment.entity;

import com.myapp.kinesis.modules.appointment.entity.AppointmentEntity;
import com.myapp.kinesis.modules.customer.entity.ClientEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_appointment", columnList = "appointment_id"),
        @Index(name = "idx_payments_status", columnList = "payment_status")
})
@Data
@NoArgsConstructor
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private AppointmentEntity appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Column(name = "payment_type")
    private String paymentType; // "FULL", "DEPOSIT", "BALANCE", "REFUND"

    @Column(name = "payment_method")
    private String paymentMethod; // "RAZORPAY", "STRIPE", "CASH", "CREDIT_PACK"

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus; // "PENDING", "COMPLETED", "FAILED", "REFUNDED"

    @Column(name = "transaction_id")
    private String transactionId; // External payment gateway ID

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}