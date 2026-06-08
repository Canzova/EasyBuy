package com.easybuy.cart_order.repositories;

import com.easybuy.cart_order.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
