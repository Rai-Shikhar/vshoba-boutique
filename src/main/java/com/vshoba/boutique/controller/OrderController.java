package com.vshoba.boutique.controller;

import com.vshoba.boutique.dto.CreateOrderRequest;
import com.vshoba.boutique.model.Order;
import com.vshoba.boutique.model.OrderStatus;
import com.vshoba.boutique.security.SecurityUser;
import com.vshoba.boutique.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Order API.
 *
 * Customers (logged in): create orders, see their own, pay after checkout.
 * Admins: see ALL orders and move them through the statuses.
 *
 * Who may call what is enforced in SecurityConfig.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ---------- Customer ----------

    /**
     * POST /api/orders - checkout. Body:
     * {
     *   "shippingAddress": "12 MG Road, Chennai",
     *   "items": [ { "productId": 1, "quantity": 2 } ]
     * }
     * Stock is deducted immediately. Order starts as PENDING.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@AuthenticationPrincipal SecurityUser currentUser,
                             @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(currentUser.getUser().getId(), request);
    }

    /**
     * GET /api/orders/mine - the logged-in customer's order history.
     */
    @GetMapping("/mine")
    public List<Order> myOrders(@AuthenticationPrincipal SecurityUser currentUser) {
        return orderService.getOrdersForUser(currentUser.getUser().getId());
    }

    /**
     * GET /api/orders/{id} - one of MY orders (404 if it is not mine).
     */
    @GetMapping("/{id}")
    public Order getMyOrder(@AuthenticationPrincipal SecurityUser currentUser,
                            @PathVariable Long id) {
        return orderService.getOrderForUser(id, currentUser.getUser().getId());
    }

    /**
     * POST /api/orders/{id}/pay - called by the frontend AFTER the
     * payment portal succeeds. PENDING -> PAID. Fails (400) if already paid.
     */
    @PostMapping("/{id}/pay")
    public Order payOrder(@AuthenticationPrincipal SecurityUser currentUser,
                          @PathVariable Long id) {
        return orderService.payOrder(id, currentUser.getUser().getId());
    }

    // ---------- Admin ----------

    /**
     * GET /api/orders - every order, newest first (admin).
     */
    @GetMapping
    public List<Order> allOrders() {
        return orderService.getAllOrders();
    }

    /**
     * PUT /api/orders/{id}/status?status=SHIPPED - move an order along
     * (admin). Valid values: PAID, SHIPPED, CANCELLED.
     */
    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return orderService.updateStatus(id, status);
    }
}
