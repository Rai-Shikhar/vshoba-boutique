package com.vshoba.boutique.exception;

/**
 * Thrown when talking to the Razorpay payment gateway fails
 * (Razorpay API error, gateway unreachable, bad credentials).
 *
 * Rendered as HTTP 500 Internal Server Error by GlobalExceptionHandler
 * - unlike BusinessRuleException (400), this is NOT the client's fault.
 */
public class RazorpayIntegrationException extends RuntimeException {

    public RazorpayIntegrationException(String message) {
        super(message);
    }
}