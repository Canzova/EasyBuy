package com.easybuy.common.events;

import lombok.*;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class OrderEvent {
    private Long orderId;
    private String orderNumber;
    private UUID userId;
    private String billingName;
    private String billingPhoneNumber;
    private String shippingAddress;
    private String paymentStatus;
    private String paymentMethod;
    private String orderStatus;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant cancelledAt;
    private String extraInfo;
}
