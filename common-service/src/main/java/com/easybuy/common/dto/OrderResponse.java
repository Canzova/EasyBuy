package com.easybuy.common.dto;
import com.easybuy.common.dto.constants.OrderStatus;
import com.easybuy.common.dto.constants.PaymentMethod;
import com.easybuy.common.dto.constants.PaymentStatus;
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
public class OrderResponse {

    private Long orderId;

    private String orderNumber;

    private UUID userId;

    private String billingName;

    private String billingPhoneNumber;

    private String shippingAddress;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    private OrderStatus orderStatus;

    private BigDecimal totalAmount;

    private Instant createdAt;
    private Instant updatedAt;

    private Instant cancelledAt;

    private String extraInfo;

    private List<ItemResponse> orderItemList = new ArrayList<>();
}

