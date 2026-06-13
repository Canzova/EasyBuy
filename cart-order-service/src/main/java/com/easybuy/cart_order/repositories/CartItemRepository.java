package com.easybuy.cart_order.repositories;

import com.easybuy.cart_order.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<Item, Long> {
}
