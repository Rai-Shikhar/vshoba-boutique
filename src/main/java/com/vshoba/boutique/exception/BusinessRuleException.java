package com.vshoba.boutique.exception;

/**
 * Thrown when an operation violates a business rule of the boutique
 * (e.g. negative stock, duplicate email, deleting a product that
 * still has stock).
 *
 * In Phase 4 this becomes an HTTP 400 Bad Request automatically.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
