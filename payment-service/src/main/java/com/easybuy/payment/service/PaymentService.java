package com.easybuy.payment.service;

import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;
import com.easybuy.payment.dto.PaymentVerificationRequest;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest paymentRequest);
    PaymentResponse verifyPayment(PaymentVerificationRequest verificationRequest);
    PaymentResponse getPaymentByOrderId(Long orderId);
    PaymentResponse getPaymentByTransactionId(String transactionId);
    void handleWebhook(String payload, String signatureHeader);
}
