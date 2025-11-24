package com.myapp.kinesis.modules.payment.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.payment.dto.PaymentRequest;
import com.myapp.kinesis.modules.payment.dto.PaymentResponse;
import com.myapp.kinesis.modules.payment.dto.RazorpayOrderResponse;
import com.myapp.kinesis.modules.payment.service.PaymentService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Creates a Razorpay order (Step 1 of payment flow).
     */
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @Valid @RequestBody PaymentRequest request) {

        RazorpayOrderResponse order = paymentService.createRazorpayOrder(request);
        return new ResponseEntity<>(ApiResponse.success(order), HttpStatus.CREATED);
    }

    /**
     * Verifies payment signature (Step 2 - called by frontend after Razorpay checkout).
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @RequestParam String razorpay_order_id,
            @RequestParam String razorpay_payment_id,
            @RequestParam String razorpay_signature) throws RazorpayException {

        PaymentResponse response = paymentService.verifyAndCompletePayment(
                razorpay_order_id,
                razorpay_payment_id,
                razorpay_signature
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}