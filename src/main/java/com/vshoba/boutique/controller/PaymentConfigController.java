package com.vshoba.boutique.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public checkout config. The frontend (payment.js) needs the Razorpay
 * key to open the checkout widget, but the key must NOT be hardcoded in
 * static JS files. It is served from the environment via properties:
 *   - prod:   razorpay.key=${RAZORPAY_KEY}  (application-prod.properties)
 *   - local:  defaults to empty (no key = simulated payment)
 *
 * Returning the key here is safe - Razorpay publishes its key in the
 * customer's browser anyway; the SECRET stays server-side only.
 */
@RestController
@RequestMapping("/api/config")
public class PaymentConfigController {

    private final String razorpayKey;

    public PaymentConfigController(@Value("${razorpay.key:}") String razorpayKey) {
        this.razorpayKey = razorpayKey;
    }

    @GetMapping("/razorpay-key")
    public Map<String, String> razorpayKey() {
        return Map.of("key", razorpayKey);
    }
}