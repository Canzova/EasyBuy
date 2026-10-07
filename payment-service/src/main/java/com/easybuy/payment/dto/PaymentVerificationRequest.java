package com.easybuy.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentVerificationRequest {

    private Long orderId;

    @NotBlank(message = "Razorpay Order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay Payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay Signature is required")
    private String razorpaySignature;
}
