package com.easybuy.cart_order.entity;

import com.easybuy.cart_order.dto.constants.CartStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
//@ToString
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private CartStatus cartStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant checkOutAt;

    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "cart",  fetch = FetchType.LAZY,  cascade = CascadeType.ALL, orphanRemoval = true)
    List<Item> itemList = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if(createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
