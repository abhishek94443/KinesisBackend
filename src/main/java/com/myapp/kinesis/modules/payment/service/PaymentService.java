package com.myapp.kinesis.modules.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.kinesis.common.exceptions.ResourceNotFoundException;
import com.myapp.kinesis.modules.appointment.entity.AppointmentEntity;
import com.myapp.kinesis.modules.appointment.repository.AppointmentRepository;
import com.myapp.kinesis.modules.payment.dto.PaymentRequest;
import com.myapp.kinesis.modules.payment.dto.PaymentResponse;
import com.myapp.kinesis.modules.payment.dto.RazorpayOrderResponse;
import com.myapp.kinesis.modules.payment.entity.PaymentEntity;
import com.myapp.kinesis.modules.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${razorpay.key_id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret:}")
    private String razorpayKeySecret;

    public PaymentService(PaymentRepository paymentRepository,
                          AppointmentRepository appointmentRepository) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Creates a Razorpay order and returns the order ID to the frontend.
     */
    @SneakyThrows
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(PaymentRequest request) {

        // 1. Validate appointment exists
        AppointmentEntity appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", request.appointmentId()));

        // 2. If Razorpay is not configured, handle as CASH
        if (razorpayKeyId == null || razorpayKeyId.isBlank()) {
            logger.warn("Razorpay not configured. Treating as CASH payment.");
            return handleCashPayment(request, appointment);
        }

        // 3. Create Razorpay order
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.amount().multiply(BigDecimal.valueOf(100)).intValue()); // Convert to paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", appointment.getId().toString());

        Order order = razorpay.orders.create(orderRequest);
        String orderId = order.get("id");

        // 4. Create payment record in PENDING state
        PaymentEntity payment = new PaymentEntity();
        payment.setAppointment(appointment);
        payment.setClient(appointment.getClient());
        payment.setAmount(request.amount());
        payment.setCurrency("INR");
        payment.setPaymentType(request.paymentType() != null ? request.paymentType() : "FULL");
        payment.setPaymentMethod("RAZORPAY");
        payment.setPaymentStatus("PENDING");
        payment.setTransactionId(orderId);

        // Store Razorpay order details in metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("razorpay_order_id", orderId);
        metadata.put("razorpay_amount", order.get("amount"));
        payment.setMetadata(objectMapper.writeValueAsString(metadata));

        paymentRepository.save(payment);

        return new RazorpayOrderResponse(orderId, request.amount(), "INR", razorpayKeyId);
    }

    /**
     * Handles CASH payment (no external gateway).
     */
    @SneakyThrows
    private RazorpayOrderResponse handleCashPayment(PaymentRequest request, AppointmentEntity appointment) {
        PaymentEntity payment = new PaymentEntity();
        payment.setAppointment(appointment);
        payment.setClient(appointment.getClient());
        payment.setAmount(request.amount());
        payment.setCurrency("INR");
        payment.setPaymentType(request.paymentType() != null ? request.paymentType() : "FULL");
        payment.setPaymentMethod("CASH");
        payment.setPaymentStatus("PENDING"); // Will be marked COMPLETED by vendor

        paymentRepository.save(payment);

        // Return a dummy order ID
        return new RazorpayOrderResponse(
                "CASH_" + payment.getId(),
                request.amount(),
                "INR",
                ""
        );
    }

    /**
     * Verifies Razorpay payment signature and marks payment as COMPLETED.
     */
    @Transactional
    public PaymentResponse verifyAndCompletePayment(String razorpayOrderId,
                                                    String razorpayPaymentId,
                                                    String razorpaySignature) throws RazorpayException {

        // 1. Find the payment record
        PaymentEntity payment = paymentRepository.findByTransactionId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", razorpayOrderId));

        // 2. Verify signature
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", razorpayOrderId);
        options.put("razorpay_payment_id", razorpayPaymentId);
        options.put("razorpay_signature", razorpaySignature);

        boolean isValid = com.razorpay.Utils.verifyPaymentSignature(options, razorpayKeySecret);

        if (!isValid) {
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment signature verification failed");
        }

        // 3. Mark as completed
        payment.setPaymentStatus("COMPLETED");
        payment.setTransactionId(razorpayPaymentId); // Update with payment ID

        PaymentEntity saved = paymentRepository.save(payment);

        logger.info("Payment completed: {} for appointment {}", razorpayPaymentId, payment.getAppointment().getId());

        return PaymentResponse.fromEntity(saved);
    }

    /**
     * Marks a CASH payment as completed (vendor confirms).
     */
    @Transactional
    public PaymentResponse markCashPaymentCompleted(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!"CASH".equals(payment.getPaymentMethod())) {
            throw new IllegalStateException("Only CASH payments can be manually marked as completed");
        }

        payment.setPaymentStatus("COMPLETED");
        PaymentEntity saved = paymentRepository.save(payment);

        return PaymentResponse.fromEntity(saved);
    }
}