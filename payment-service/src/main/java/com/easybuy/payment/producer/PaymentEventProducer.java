package com.easybuy.payment.producer;

import com.easybuy.common.dto.constants.PaymentStatus;
import com.easybuy.common.events.PaymentEvent;
import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.payment.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private static final String PAYMENT_EVENT_TOPIC = "PAYMENT_EVENT";
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    /**
     * Publishes a PaymentEvent directly to Kafka topic PAYMENT_EVENT.
     */
    public void publishPaymentEvent(PaymentEvent paymentEvent) {
        log.info("Publishing PaymentEvent to Kafka topic {}: {}", PAYMENT_EVENT_TOPIC, paymentEvent);
        try {
            kafkaTemplate.send(PAYMENT_EVENT_TOPIC, paymentEvent);
            log.info("PaymentEvent sent successfully for Order ID: {}", paymentEvent.getOrderId());
        } catch (Exception e) {
            log.error("Exception while sending PaymentEvent {}: {}", paymentEvent, e.getMessage(), e);
            throw new BusinessException("Payment Event Send Failed.", e);
        }
    }

    /**
     * Constructs and publishes a PaymentEvent from a Transaction entity and status.
     */
    public void publishPaymentEvent(Transaction transaction, PaymentStatus status) {
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .orderId(transaction.getOrderId())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .status(status)
                .paymentGatewayTxnId(transaction.getPaymentGatewayTxnId())
                .paymentGatewayOrderId(transaction.getPaymentGatewayOrderId())
                .paymentGatewaySignature(transaction.getPaymentGatewaySignature())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();

        publishPaymentEvent(paymentEvent);
    }

    /**
     * Alias method for backwards compatibility.
     */
    public void paymentEventProducer(PaymentEvent paymentEvent) {
        publishPaymentEvent(paymentEvent);
    }
}
