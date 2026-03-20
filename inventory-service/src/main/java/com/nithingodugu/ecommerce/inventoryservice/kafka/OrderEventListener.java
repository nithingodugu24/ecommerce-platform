package com.nithingodugu.ecommerce.inventoryservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.event.*;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import com.nithingodugu.ecommerce.inventoryservice.exceptions.DuplicateReleaseException;
import com.nithingodugu.ecommerce.inventoryservice.exceptions.ReservationNotFoundException;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryRepository;
import com.nithingodugu.ecommerce.inventoryservice.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = {"order.cancelled", "order.failed"})
    @Transactional
    public void handleOrderCancelled(
            @Payload String payload,
            Acknowledgment ack
    ) {

        try {
            OrderCancelledEvent event =
                    objectMapper.readValue(payload, OrderCancelledEvent.class);

            log.info("OrderCancelledEvent received",
                    kv("orderId", event.getOrderId())
            );

            inventoryService.processOrderCancelled(event);

            log.info("OrderCancelledEvent processed",
                    kv("orderId", event.getOrderId())
            );

            ack.acknowledge();

        } catch (JsonProcessingException ex) {

            log.error("Deserialization failed for OrderCancelledEvent",
                    kv("payload", safePayload(payload)),
                    kv("error", ex.getMessage()),
                    ex
            );

        } catch (ReservationNotFoundException ex) {

            log.warn("OrderCancelledEvent failed - reservation not found",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();

        } catch (DuplicateReleaseException ex) {

            log.warn("Duplicate inventory release event",
                    kv("orderId", extractOrderIdSafe(payload))
            );
            ack.acknowledge();

        } catch (Exception ex) {

            log.error("Unexpected error processing OrderCancelledEvent",
                    kv("payload", safePayload(payload)),
                    ex
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
