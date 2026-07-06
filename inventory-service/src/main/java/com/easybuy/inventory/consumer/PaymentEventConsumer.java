package com.easybuy.inventory.consumer;

import com.easybuy.common.dto.constants.PaymentStatus;
import com.easybuy.common.events.PaymentEvent;
import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final InventoryService inventoryService;

    // This is topic name
    private static final String PAYMENT_EVENT = "PAYMENT_EVENT";

    // This is the name of the consumer group listening to the PAYMENT_EVENT Topic
    private static final String GROUP_ID = "inventory-group";

    @KafkaListener(topics = PAYMENT_EVENT, groupId = GROUP_ID)
    public void consume(PaymentEvent paymentEvent){
        log.info("Received Payment Event: {}", paymentEvent);

        if(paymentEvent == null){
            log.info("Payment Event not found");
            return;
        }

        if(paymentEvent.getStatus() == PaymentStatus.PAID){
            log.info("Payment Status is PAID");
            return;
        }

        if(paymentEvent.getStatus() == PaymentStatus.PENDING){
            log.info("Payment Status is PENDING");
            return;
        }

        try{
            // Now we will process failed payment status
            log.info("Handling failed Payment Status.");
            inventoryService.handleFailedPayment(paymentEvent.getOrderId());
            log.info("Completed Compensating transactions.");
        }catch (Exception e){
            log.error("Exception occurred while doing Compensating transactions for orderId : {}, paymentId : {}", paymentEvent.getOrderId(), paymentEvent.getId(), e);
            throw new BusinessException(e.getMessage(), e);
        }

    }

}
