package com.easybuy.cart_order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="orderItem")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
//@ToString
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @Column(nullable = false, length = 120)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @ManyToOne()
    @JoinColumn(name = "oderId")
    private Order order;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer discountPercentage;

    @Column(nullable = false)
    private BigDecimal discountedPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalOrderItemPrice;


}
