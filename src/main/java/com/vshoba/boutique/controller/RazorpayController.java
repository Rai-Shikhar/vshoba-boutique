package com.vshoba.boutique.controller;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.vshoba.boutique.dto.CreateRazorpayOrderRequest;
import com.vshoba.boutique.dto.VerifyPaymentRequest;
import com.vshoba.boutique.exception.BusinessRuleException;
import com.vshoba.boutique.exception.RazorpayIntegrationException;
import com.vshoba.boutique.model.Order;
import com.vshoba.boutique.model.OrderStatus;
import com.vshoba.boutique.security.SecurityUser;
import com.vshoba.boutique.service.OrderService;
import jakarta.validation.Valid;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * Razorpay Standard Web Checkout integration (server-side flow).
 *
 *   POST /api/razorpay/create-order  - create a Razorpay order for the
 *                                      caller's pending order, get an
 *                                      order_id for the checkout modal.
 *   POST /api/razorpay/verify-payment - HMAC-SHA256 check of the values
 *                                      returned by the modal; only a
 *                                      valid signature marks the order PAID.
 *
 * Credentials never appear in code: key id/secret come from
 *   RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET (env vars, or the local .env
 *   file during development via spring-dotenv). Only the KEY_ID ever
 *   reaches the browser (through /api/config/razorpay-key).
 */
@RestController
@RequestMapping("/api/razorpay")
public class RazorpayController {

    /** Razorpay's minimum supported order amount: 100 paise = Rs 1. */
    private static final long MIN_AMOUNT_PAISE = 100L;

    private final OrderService orderService;
    private final String keyId;
    private final String keySecret;
    private final RazorpayClient razorpayClient;

    public RazorpayController(OrderService orderService,
                              @Value("${razorpay.key-id:}") String keyId,
                              @Value("${razorpay.key-secret:}") String keySecret) {
        this.orderService = orderService;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.razorpayClient = buildClient(keyId, keySecret);
    }

    private static RazorpayClient buildClient(String keyId, String keySecret) {
        if (keyId.isBlank() || keySecret.isBlank()) {
            return null; // not configured -> endpoints answer with a clean 400
        }
        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new RazorpayIntegrationException("Invalid Razorpay credentials: " + e.getMessage());
        }
    }

    /**
     * Create a Razorpay order. The amount is NOT taken from the request -
     * it is read from the caller's order in the database, so a client can
     * never under-pay by editing the request body.
     */
    @PostMapping("/create-order")
    public Map<String, String> createOrder(@AuthenticationPrincipal SecurityUser currentUser,
                                           @Valid @RequestBody CreateRazorpayOrderRequest request) {
        if (razorpayClient == null) {
            throw new BusinessRuleException("Razorpay is not configured on this server");
        }

        Order order = orderService.getOrderForUser(request.orderId(), currentUser.getUser().getId());
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only pending orders can be paid (current status: " + order.getStatus() + ")");
        }

        long amountPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        if (amountPaise < MIN_AMOUNT_PAISE) {
            throw new BusinessRuleException("Order amount is below the minimum online payment of Rs 1");
        }

        JSONObject options = new JSONObject();
        options.put("amount", amountPaise);
        options.put("currency", "INR");
        options.put("receipt", "boutique_order_" + order.getId());
        options.put("payment_capture", 1); // capture money the moment payment succeeds

        com.razorpay.Order razorpayOrder;
        try {
            razorpayOrder = razorpayClient.orders.create(options);
        } catch (RazorpayException e) {
            throw new RazorpayIntegrationException("Razorpay order creation failed: " + e.getMessage());
        }

        String razorpayOrderId = (String) razorpayOrder.get("id");

        return Map.of(
                "order_id", razorpayOrderId,
                "amount", String.valueOf(amountPaise),
                "currency", "INR",
                "key", keyId
        );
    }

    /**
     * Verify the payment signature returned by the checkout modal.
     * signature = Base64(HMAC-SHA256(order_id | payment_id, KEY_SECRET)).
     * The order is only marked PAID when the signatures match.
     */
    @PostMapping("/verify-payment")
    public Order verifyPayment(@AuthenticationPrincipal SecurityUser currentUser,
                               @Valid @RequestBody VerifyPaymentRequest request) {
        if (keySecret.isBlank()) {
            throw new BusinessRuleException("Razorpay is not configured on this server");
        }

        boolean signatureValid = verifySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature());

        if (!signatureValid) {
            throw new BusinessRuleException("Payment signature verification failed - payment not confirmed");
        }

        return orderService.payOrder(request.orderId(), currentUser.getUser().getId());
    }

    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RazorpayIntegrationException("Could not verify payment signature: " + e.getMessage());
        }
    }
}