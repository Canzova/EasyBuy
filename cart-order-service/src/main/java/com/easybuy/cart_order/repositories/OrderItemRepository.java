package com.easybuy.cart_order.repositories;

import com.easybuy.cart_order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
