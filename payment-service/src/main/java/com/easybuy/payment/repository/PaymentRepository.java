package com.easybuy.payment.repository;

import com.easybuy.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Transaction, Long> {
}
