package com.vshoba.boutique.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of POST /api/razorpay/create-order.
 *
 * The caller sends OUR database order id; the backend loads the order,
 * verifies it belongs to the caller, and tells Razorpay the real amount
 * in paise. The amount is never trusted from the client.
 */
public record CreateRazorpayOrderRequest(

        @NotNull(message = "Order id is required")
        Long orderId
) {
}