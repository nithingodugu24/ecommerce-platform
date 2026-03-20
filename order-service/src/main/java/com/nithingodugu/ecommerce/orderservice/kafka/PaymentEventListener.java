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

import static net.logstash.logback.argument.StructuredArguments.kv;

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
    ) {
        try {
            PaymentAuthorizedEvent event =
                    objectMapper.readValue(payload, PaymentAuthorizedEvent.class);

            log.info("PaymentAuthorizedEvent received",
                    kv("orderId", event.orderId())
            );

            orderService.processPaymentSuccessOrder(event.orderId());

            log.info("PaymentAuthorizedEvent processed",
                    kv("orderId", event.orderId())
            );

            ack.acknowledge();

        } catch (JsonProcessingException ex) {
            log.error("Deserialization failed for PaymentAuthorizedEvent",
                    kv("payload", safePayload(payload)),
                    kv("error", ex.getMessage()),
                    ex
            );

        } catch (OrderNotFoundException ex) {
            log.warn("PaymentAuthorizedEvent failed - order not found",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();

        } catch (DuplicateOrderStateException ex) {
            log.warn("Duplicate payment authorization event",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Unexpected error processing PaymentAuthorizedEvent",
                    kv("payload", safePayload(payload)),
                    ex
            );
        }
    }

    @KafkaListener(topics = "payment.failed")
    @Transactional
    public void handlePaymentFailed(
            @Payload String payload,
            Acknowledgment ack
    ) {
        try {
            PaymentFailedEvent event =
                    objectMapper.readValue(payload, PaymentFailedEvent.class);

            log.info("PaymentFailedEvent received",
                    kv("orderId", event.orderId())
            );

            orderService.processPaymentFailedOrder(event.orderId());

            log.info("PaymentFailedEvent processed",
                    kv("orderId", event.orderId())
            );

            ack.acknowledge();

        } catch (JsonProcessingException ex) {
            log.error("Deserialization failed for PaymentFailedEvent",
                    kv("payload", safePayload(payload)),
                    kv("error", ex.getMessage())
            );

        } catch (OrderNotFoundException ex) {
            log.warn("PaymentFailedEvent failed - order not found",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();

        } catch (DuplicateOrderStateException ex) {
            log.warn("Duplicate payment failed event",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Unexpected error processing PaymentFailedEvent",
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
