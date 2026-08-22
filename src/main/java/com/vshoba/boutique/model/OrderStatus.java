package com.vshoba.boutique.model;

/**
 * Lifecycle of an order:
 *   PENDING  - placed, not paid yet
 *   PAID     - payment confirmed
 *   SHIPPED  - handed to the courier
 *   CANCELLED - cancelled (refund handling comes later)
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    CANCELLED
}
