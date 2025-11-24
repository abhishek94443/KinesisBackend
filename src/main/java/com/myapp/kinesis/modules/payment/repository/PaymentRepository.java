package com.myapp.kinesis.modules.payment.repository;

import com.myapp.kinesis.modules.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    List<PaymentEntity> findAllByAppointmentId(UUID appointmentId);

    Optional<PaymentEntity> findByTransactionId(String transactionId);

    List<PaymentEntity> findAllByClientId(UUID clientId);
}