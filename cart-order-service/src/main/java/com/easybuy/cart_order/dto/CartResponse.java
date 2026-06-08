package com.easybuy.cart_order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {
    private Long cartId;

    private UUID userId;

    private CartStatus cartStatus;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant checkOutAt;

    private BigDecimal cartTotalPrice;

    List<CartItemResponse> cartItemList = new ArrayList<>();
}
