package com.nithingodugu.ecommerce.orderservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.event.PaymentAuthorizedEvent;
import com.nithingodugu.ecommerce.common.event.PaymentFailedEvent;
import com.nithingodugu.ecommerce.orderservice.exceptions.DuplicateOrderStateException;
import com.nithingodugu.ecommerce.orderservice.exceptions.OrderNotFoundException;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;



    @KafkaListener(topics = "payment.authorized")
    public void handlePaymentAuthorized(
            @Payload String payload,
            Acknowledgment ack
    ){
        try {
            PaymentAuthorizedEvent event = objectMapper.readValue(payload, PaymentAuthorizedEvent.class);
            orderService.processPaymentSuccessOrder(event.orderId());
            ack.acknowledge();
        }catch (JsonProcessingException ex) {
            log.error("Failed to deserialize PaymentAuthorizedEvent payload={}", payload, ex);

        }catch (OrderNotFoundException ex){
            log.error("Invalid OrderId=,{}", ex);
            ack.acknowledge();
        }catch (DuplicateOrderStateException ex){
            log.error("Recieved duplicate order to confirm of id={}", ex);
            ack.acknowledge();
        }catch (Exception ex){
            log.error("Failed to handle PaymentAuthorizedEvent payload={}", payload, ex);
        }


    }

    @KafkaListener(topics = "payment.failed")
    @Transactional
    public void handlePaymentFailed(
            @Payload String payload,
            Acknowledgment ack
    ){
        try {
            PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
            orderService.processPaymentFailedOrder(event.orderId());
            ack.acknowledge();
        }catch (JsonProcessingException ex) {
            log.error("Failed to deserialize PaymentFailedEvent payload={}", payload, ex);

        }catch (OrderNotFoundException ex){
            log.error("Invalid OrderId=,{}", ex);
            ack.acknowledge();
        }catch (DuplicateOrderStateException ex){
            log.error("Recieved duplicate order of id={}", ex);
            ack.acknowledge();
        }catch (Exception ex){
            log.error("Failed to handle PaymentFailedEvent payload={}", payload, ex);
        }
    }

}
