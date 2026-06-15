package com.easybuy.cart_order.repositories;

import com.easybuy.cart_order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumberOrderByCreatedAtDesc(String orderNumber);
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
