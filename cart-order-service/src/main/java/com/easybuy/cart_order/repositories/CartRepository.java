package com.easybuy.cart_order.repositories;

import com.easybuy.cart_order.dto.CartStatus;
import com.easybuy.cart_order.entity.Cart;
import com.easybuy.cart_order.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndCartStatus(UUID userId, CartStatus cartStatus);
}
