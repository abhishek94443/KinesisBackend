package com.myapp.kinesis.modules.payment.dto;

import java.math.BigDecimal;

/**
 * Response sent to frontend to initiate Razorpay checkout.
 */
public record RazorpayOrderResponse(
        String orderId,
        BigDecimal amount,
        String currency,
        String razorpayKeyId
) {
}