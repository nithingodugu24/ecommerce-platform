package com.nithingodugu.ecommerce.paymentservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.paymentservice.exceptions.DuplicateRefundException;
import com.nithingodugu.ecommerce.paymentservice.exceptions.PaymentNotFoundException;
import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @KafkaListener(topics = "order.cancelled")
    public void handleOrderCancelled(
            @Payload String payload,
            Acknowledgment ack
    ){
        try {
            OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
            paymentService.processRefund(event);
            ack.acknowledge();
        }catch (JsonProcessingException ex){
            log.error("Failed to deserialize PaymentAuthorizedEvent payload={}", payload, ex);
        }catch (PaymentNotFoundException ex) {
            log.error("Payment not found in OrderCancelledEvent {}", ex);
            ack.acknowledge();
        }catch (DuplicateRefundException ex){
            log.error("Payment already refunded {}", ex);
            ack.acknowledge();
        }catch (Exception ex){
            log.error("Failed to handle ProductCreatedEvent payload={}", payload, ex);
        }
    }
}
