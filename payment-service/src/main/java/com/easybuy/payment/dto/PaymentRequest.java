package com.easybuy.payment.dto;

import com.easybuy.common.dto.constants.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class PaymentRequest {
    @NotNull(message = "Order Id is required.")
    private Long orderId;

    @NotNull(message = "Total amount is required.")
    @Positive(message = "Amount must be positive.")
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Payment method is required.")
    private PaymentMethod paymentMethod;

    private String paymentDetails;
}
