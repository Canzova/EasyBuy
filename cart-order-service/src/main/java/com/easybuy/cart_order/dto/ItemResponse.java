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
public class ItemResponse {

    private Long itemId;

    private UUID productId;

    private String productName;

    private BigDecimal unitPrice;

    private Integer discountPercentage;

    private BigDecimal discountedPrice;

    private Integer quantity;

    private BigDecimal itemTotalPrice;

}
