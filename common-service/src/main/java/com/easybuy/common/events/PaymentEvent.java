package com.easybuy.common.events;

import com.easybuy.common.dto.constants.PaymentMethod;
import com.easybuy.common.dto.constants.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class PaymentEvent {
    private Long id;
    private String transactionId;
    private Long orderId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String paymentGatewayTxnId;
    private  String paymentGatewayOrderId;
    private  String paymentGatewaySignature;
    private Instant createdAt;
    private Instant updatedAt;
}
