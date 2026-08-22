package com.vshoba.boutique.repository;

import com.vshoba.boutique.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for orders.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * A customer's order history, newest first.
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * One order, but only if it belongs to this user.
     * This is how customers fetch their OWN orders - it is impossible
     * to look at someone else's by guessing ids.
     */
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
