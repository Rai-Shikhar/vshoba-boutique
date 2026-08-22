package com.vshoba.boutique.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of POST /api/orders (checkout):
 * {
 *   "shippingAddress": "12 MG Road, Chennai",
 *   "items": [
 *     { "productId": 1, "quantity": 2 },
 *     { "productId": 3, "quantity": 1 }
 *   ]
 * }
 */
public record CreateOrderRequest(

        @NotBlank(message = "Shipping address is required")
        @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
        String shippingAddress,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {
}
