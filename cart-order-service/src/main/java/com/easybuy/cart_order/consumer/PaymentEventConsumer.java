package com.easybuy.cart_order.consumer;

import com.easybuy.cart_order.Service.OrderService;
import com.easybuy.common.events.PaymentEvent;
import com.easybuy.common.exceptions.customException.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private static final String PAYMENT_EVENT = "PAYMENT_EVENT";
    private final OrderService orderService;

    @KafkaListener(topics = PAYMENT_EVENT, groupId = "order-group")
    public void consumePaymentEvent(PaymentEvent paymentEvent) {
        log.info("Inside consumePaymentEvent method");

        if(paymentEvent == null){
            log.info("Payment event is null");
            return;
        }

        try{
            log.info("Updating order payment status with kafka for orderId {} and status {}",  paymentEvent.getOrderId(), paymentEvent.getStatus());
            orderService.updateOrderStatus(paymentEvent.getOrderId(), paymentEvent.getStatus().toString());
            log.info("Payment event updated successfully");
        }catch (Exception e){
            log.info("Error while updating order payment status", e);
            throw new BusinessException("Error while updating order payment status", e);
        }

    }
}
