package com.easybuy.payment.repository;

import com.easybuy.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByOrderId(Long orderId);
    Optional<Transaction> findByPaymentGatewayOrderId(String paymentGatewayOrderId);
    Optional<Transaction> findByTransactionId(String transactionId);
    Optional<Transaction> findByPaymentGatewayTxnId(String paymentGatewayTxnId);
}
