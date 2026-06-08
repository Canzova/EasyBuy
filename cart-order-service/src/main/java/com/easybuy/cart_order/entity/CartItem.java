package com.easybuy.cart_order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cartItem")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartId")
    private Cart cart;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    private Integer discountPercentage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountedPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cartItemTotalPrice;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if(quantity == null){
            quantity = 0;
        }
        if(discountPercentage == null){
            discountPercentage = 0;
        }
        if(unitPrice == null){
            unitPrice = BigDecimal.ZERO;
        }
        if(discountedPrice == null){
            discountedPrice = BigDecimal.ZERO;
        }

        cartItemTotalPrice = discountedPrice.multiply(BigDecimal.valueOf(quantity));
    }

}
