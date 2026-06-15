package com.easybuy.cart_order.entity;

import com.easybuy.cart_order.dto.constants.OrderStatus;
import com.easybuy.cart_order.dto.constants.PaymentMethod;
import com.easybuy.cart_order.dto.constants.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
//@ToString
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false, unique = true, length = 36)
    private String orderNumber;

    @JdbcTypeCode(SqlTypes.VARCHAR) // Store this uuid as a varchar
    @Column(nullable = false, length = 36)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String billingName;

    @Column(nullable = false, length = 13)
    private String billingPhoneNumber;

    @Column(nullable = false, length = 400)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    private Instant createdAt;
    private Instant updatedAt;

    private Instant cancelledAt;

    @Column(columnDefinition = "Text")
    private String extraInfo;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 15)
    private List<OrderItem> orderItemList = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if(this.createdAt == null) {
            this.createdAt = now;
        }

        this.updatedAt = now;

        if(this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }

        if(this.orderStatus == null) {
            this.orderStatus = OrderStatus.CONFIRMED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

}
