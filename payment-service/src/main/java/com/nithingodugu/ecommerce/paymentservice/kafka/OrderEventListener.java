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

import static net.logstash.logback.argument.StructuredArguments.kv;

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

            log.info("OrderCancelledEvent received",
                    kv("orderId", event.getOrderId())
            );

            paymentService.processRefund(event);

            log.info("OrderCancelledEvent processed",
                    kv("orderId", event.getOrderId())
            );

            ack.acknowledge();
        }catch (JsonProcessingException ex){
            log.error("Deserialization failed for OrderCancelledEvent",
                    kv("payload", safePayload(payload)),
                    kv("error", ex.getMessage())
            );
        }catch (PaymentNotFoundException ex) {
            log.warn("OrderCancelledEvent failed - payment not found",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();
        }catch (DuplicateRefundException ex){
            log.warn("Duplicate refund event",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();
        }catch (Exception ex){
            log.error("Unexpected error processing OrderCancelledEvent",
                    kv("payload", safePayload(payload)),
                    kv("error", ex)
            );
        }
    }

    private String safePayload(String payload) {
        return payload.length() > 500 ? payload.substring(0, 500) + "..." : payload;
    }

    private String extractOrderIdSafe(String payload) {
        try {
            return objectMapper.readTree(payload).path("orderId").asText("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
