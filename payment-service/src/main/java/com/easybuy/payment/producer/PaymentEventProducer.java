package com.easybuy.payment.producer;

import com.easybuy.common.events.PaymentEvent;
import com.easybuy.common.exceptions.customException.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    // This is topic name
    private static final String PAYMENT_EVENT = "PAYMENT_EVENT";
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void paymentEventProducer(PaymentEvent paymentEvent) {
        log.info("Starting paymentEventProducer method");

        try{
            log.info("Sending paymentEvent {}", paymentEvent);
            // This .send is async call which return CompletableFuture
            kafkaTemplate.send(PAYMENT_EVENT, paymentEvent);
            log.info("PaymentEvent sent successfully");
        }catch (Exception e){
            log.debug("Exception while sending paymentEvent {}, {}", paymentEvent, e.getMessage());
            throw new BusinessException("Payment Event Send Failed.", e);
        }
    }

}
