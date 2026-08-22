package com.vshoba.boutique.service;

import com.vshoba.boutique.dto.CreateOrderRequest;
import com.vshoba.boutique.dto.OrderItemRequest;
import com.vshoba.boutique.exception.BusinessRuleException;
import com.vshoba.boutique.exception.ResourceNotFoundException;
import com.vshoba.boutique.model.Order;
import com.vshoba.boutique.model.OrderItem;
import com.vshoba.boutique.model.OrderStatus;
import com.vshoba.boutique.model.Product;
import com.vshoba.boutique.model.User;
import com.vshoba.boutique.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout and order management.
 *
 * createOrder is the heart of the boutique's money flow, and it is ONE
 * transaction: if any line fails (e.g. one item has no stock), NOTHING
 * is saved - no order, and no stock taken from the other items.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final UserService userService;

    public OrderService(OrderRepository orderRepository,
                        ProductService productService,
                        UserService userService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.userService = userService;
    }

    // ---------- Customer ----------

    /**
     * Place an order. For every line:
     *   1. product must exist and be active
     *   2. stock must be enough - otherwise the WHOLE order is rejected
     *   3. stock is deducted immediately
     * Prices are snapshotted into the order so later price changes
     * never rewrite history.
     */
    @Transactional
    public Order createOrder(Long userId, CreateOrderRequest request) {
        User user = userService.getUserById(userId);

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.shippingAddress().trim());

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productService.getActiveProductById(itemRequest.productId());

            if (product.getStockQuantity() < itemRequest.quantity()) {
                throw new BusinessRuleException("Not enough stock for '" + product.getName()
                        + "' (only " + product.getStockQuantity() + " left)");
            }

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(itemRequest.quantity());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
            order.addItem(item);

            total = total.add(item.getSubtotal());

            product.setStockQuantity(product.getStockQuantity() - itemRequest.quantity());
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    /**
     * The logged-in customer's own order history, newest first.
     */
    public List<Order> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * One order, but ONLY if it belongs to this user.
     * Other users' orders simply do not exist from the caller's view.
     */
    public Order getOrderForUser(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order with id " + orderId + " does not exist"));
    }

    /**
     * Called by the frontend AFTER the payment portal confirms success.
     * Only a PENDING order can be paid - this prevents paying twice.
     */
    @Transactional
    public Order payOrder(Long orderId, Long userId) {
        Order order = getOrderForUser(orderId, userId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Only pending orders can be paid (current status: " + order.getStatus() + ")");
        }
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }

    // ---------- Admin ----------

    /**
     * Every order, newest first - grandma's order book.
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Move an order to the next stage (e.g. PENDING -> PAID -> SHIPPED).
     */
    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " does not exist"));
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}
