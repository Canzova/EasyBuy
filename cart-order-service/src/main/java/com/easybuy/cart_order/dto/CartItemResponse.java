package com.easybuy.cart_order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponse {

    private Long cartItemId;

    private UUID productId;

    private String productName;

    private BigDecimal unitPrice;

    private Integer discountPercentage;

    private Integer discountedPrice;

    private Integer quantity;

    private BigDecimal cartItemTotalPrice;

}
