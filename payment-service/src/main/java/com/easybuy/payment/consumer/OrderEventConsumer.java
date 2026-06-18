package com.easybuy.payment.consumer;

import com.easybuy.common.dto.constants.PaymentMethod;
import com.easybuy.common.dto.constants.PaymentStatus;
import com.easybuy.common.events.OrderEvent;
import com.easybuy.common.events.PaymentEvent;
import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;
import com.easybuy.payment.producer.PaymentEventProducer;
import com.easybuy.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final String ORDER_TOPIC = "ORDER_EVENT";
    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(topics = ORDER_TOPIC, groupId = "payment-group")
    public void consumeOrderEvent(OrderEvent orderEvent) {
        log.info("Order event received: {}", orderEvent);

        if(orderEvent == null){
            log.info("Order event received is null");
            return;
        }

        log.info("Creating paymentRequest.");
        try{
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(orderEvent.getOrderId())
                    .totalAmount(orderEvent.getTotalAmount())
                    .paymentMethod(PaymentMethod.valueOf(orderEvent.getPaymentMethod()))
                    .paymentDetails("This is payment details.")
                    .build();

            log.info("Payment request created: {}", paymentRequest);

            log.info("Calling processPayment method of paymentService with paymentRequest");
            PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);

            // Now create a payment event and publish it with PaymentEventProducer
            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .id(paymentResponse.getId())
                    .amount(paymentResponse.getAmount())
                    .transactionId(paymentResponse.getTransactionId())
                    .orderId(paymentResponse.getOrderId())
                    .paymentGatewayTxnId(paymentResponse.getPaymentGatewayTxnId())
                    .paymentGatewayOrderId(paymentResponse.getPaymentGatewayOrderId())
                    .paymentGatewaySignature(paymentResponse.getPaymentGatewaySignature())
                    .amount(paymentResponse.getAmount())
                    .status(paymentResponse.getStatus())
                    .createdAt(paymentResponse.getCreatedAt())
                    .updatedAt(paymentResponse.getUpdatedAt())
                    .build();

            log.info("PaymentEvent created: {}", paymentEvent);

            log.info("Sending paymentEvent");
            paymentEventProducer.paymentEventProducer(paymentEvent);
        }catch (Exception e){
            log.error("Error occurred while sending paymentEvent", e);

            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .orderId(orderEvent.getOrderId())
                    .status(PaymentStatus.FAILED)
                    .build();

            log.info("Failed Payment Event created: {}", paymentEvent);
            paymentEventProducer.paymentEventProducer(paymentEvent);
            log.info("Sent Failed Payment Event");
        }
    }

}
