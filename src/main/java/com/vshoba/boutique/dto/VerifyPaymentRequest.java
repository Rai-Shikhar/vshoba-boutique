package com.vshoba.boutique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body of POST /api/razorpay/verify-payment.
 *
 * The checkout returns these three values after a successful payment:
 *   razorpay_order_id, razorpay_payment_id, razorpay_signature
 * The backend re-computes the HMAC-SHA256 signature and only marks the
 * order PAID if it matches.
 */
public record VerifyPaymentRequest(

        @NotNull(message = "Order id is required")
        Long orderId,

        @NotBlank(message = "razorpay_order_id is required")
        String razorpayOrderId,

        @NotBlank(message = "razorpay_payment_id is required")
        String razorpayPaymentId,

        @NotBlank(message = "razorpay_signature is required")
        String razorpaySignature
) {
}