package com.easybuy.payment.service;

import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest paymentRequest);
}
