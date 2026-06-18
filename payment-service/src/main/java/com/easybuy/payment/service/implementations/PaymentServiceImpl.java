package com.easybuy.payment.service.implementations;

import com.easybuy.common.dto.constants.PaymentStatus;
import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;
import com.easybuy.payment.entity.Transaction;
import com.easybuy.payment.repository.PaymentRepository;
import com.easybuy.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository transactionRepository;

    @Override
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment for Order ID: {} with amount: {}", paymentRequest.getOrderId(), paymentRequest.getTotalAmount());

        // Validate payment details
        if (paymentRequest.getPaymentDetails() == null || paymentRequest.getPaymentDetails().trim().isEmpty()) {
            throw new BusinessException("Payment details (card/wallet info) are required");
        }

        Transaction transaction = new Transaction();
        transaction.setOrderId(paymentRequest.getOrderId());
        transaction.setAmount(paymentRequest.getTotalAmount());
        transaction.setPaymentMethod(paymentRequest.getPaymentMethod());
        transaction.setTransactionId(UUID.randomUUID().toString());

        // Standard simulation: check for fail keywords or specific mock failed card numbers
        String details = paymentRequest.getPaymentDetails().toLowerCase();
        if (details.contains("fail") || details.contains("1111-1111-1111-1111") || details.contains("error")) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setPaymentGatewayTxnId("GATEWAY-FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            Transaction saved = transactionRepository.save(transaction);
            log.warn("Payment failed for Order ID: {}. Gateway Txn: {}", paymentRequest.getOrderId(), saved.getPaymentGatewayTxnId());
            throw new BusinessException("Payment failed via gateway: transaction declined");
        }

        //TODO: actual logic:---- payment gateway call karnge

        // Simulate successful payment processing
        transaction.setStatus(PaymentStatus.PAID);
        transaction.setPaymentGatewayTxnId("GATEWAY-PAID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setPaymentGatewaySignature("GATEWAY-SIGNATURE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setPaymentGatewayOrderId("GATEWAY-ORDERID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        Transaction saved = transactionRepository.save(transaction);
        log.info("Payment processed successfully for Order ID: {}. Txn ID: {}, Gateway Txn: {}",
                paymentRequest.getOrderId(), saved.getTransactionId(), saved.getPaymentGatewayTxnId());

        return transactionToPaymentResponse(transaction);
    }

    private PaymentResponse transactionToPaymentResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .id(transaction.getId())
                .orderId(transaction.getOrderId())
                .amount(transaction.getAmount())
                .paymentGatewayTxnId(transaction.getPaymentGatewayTxnId())
                .paymentGatewayOrderId(transaction.getPaymentGatewayOrderId())
                .paymentGatewaySignature(transaction.getPaymentGatewaySignature())
                .transactionId(transaction.getTransactionId())
                .createdAt(transaction.getCreatedAt())
                .status(transaction.getStatus())
                .updatedAt(transaction.getUpdatedAt())
                .paymentMethod(transaction.getPaymentMethod())
                .build();
    }
}
