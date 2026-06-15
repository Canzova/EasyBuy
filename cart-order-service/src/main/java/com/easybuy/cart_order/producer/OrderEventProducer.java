package com.easybuy.cart_order.producer;

import com.easybuy.common.events.OrderEvent;
import com.easybuy.common.exceptions.customException.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String ORDER_TOPIC = "ORDER_EVENT";

    public void produceOrderEvent(OrderEvent orderEvent) {
        try {
            kafkaTemplate.send(ORDER_TOPIC, orderEvent);
            log.info("Order event has been sent to topic: {} with order event : {}",ORDER_TOPIC, orderEvent);
        }catch (Exception e){
            log.error("send order event error", e);
            throw new BusinessException("send order event error");
        }
    }

}
