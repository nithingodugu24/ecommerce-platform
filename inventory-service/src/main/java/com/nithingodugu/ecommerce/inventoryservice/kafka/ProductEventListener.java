package com.nithingodugu.ecommerce.inventoryservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nithingodugu.ecommerce.inventoryservice.event.ProductCreatedEvent;
import com.nithingodugu.ecommerce.common.event.ProductDeletedEvent;
import com.nithingodugu.ecommerce.inventoryservice.exceptions.DuplicateInventoryException;
import com.nithingodugu.ecommerce.inventoryservice.exceptions.InventoryNotFoundException;
import com.nithingodugu.ecommerce.inventoryservice.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@AllArgsConstructor
public class ProductEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product.created")
    public void handleProductCreated(
            @Payload String payload,
            Acknowledgment ack
    ){

        try {
            ProductCreatedEvent event = objectMapper.readValue(payload, ProductCreatedEvent.class);

            log.info("ProductCreatedEvent received",
                    kv("productId", event.getProductId()),
                    kv("quantity", event.getInitialQuantity())
            );

            inventoryService.handleProductCreated(event);

            log.info("ProductCreatedEvent processed",
                    kv("productId", event.getProductId())
            );

            ack.acknowledge();

        }catch (JsonProcessingException ex) {
            log.error("Deserialization failed for ProductCreatedEvent",
                    kv("payload", safePayload(payload)),
                    kv("error", ex.getMessage()),
                    ex
            );

        }catch (DuplicateInventoryException ex) {
            log.warn("Inventory already exists",
                    kv("productId", ex.getMessage())
            );

            ack.acknowledge();

        }catch (Exception ex){
            log.error("Unexpected error processing ProductCreatedEvent",
                    kv("payload", safePayload(payload)),
                    ex
            );
        }
    }

    @KafkaListener(topics = "product.deleted")
    public void handleProductDeleted(
            @Payload String payload,
            Acknowledgment ack
    ){
        try {
            ProductDeletedEvent event = objectMapper.readValue(payload, ProductDeletedEvent.class);

            log.info("ProductDeletedEvent received",
                    kv("productId", event.getProductId())
            );

            inventoryService.handleProductDeleted(event);

            log.info("ProductDeletedEvent processed",
                    kv("productId", event.getProductId())
            );

            ack.acknowledge();

        }catch (JsonProcessingException ex) {
            log.error("Deserialization failed for ProductDeletedEvent",
                    kv("payload", safePayload(payload)),
                    kv("error", ex.getMessage()),
                    ex
            );

        }catch (InventoryNotFoundException ex) {
            log.warn("Inventory not found",
                    kv("productId", ex.getMessage())
            );

            ack.acknowledge();

        }catch (Exception ex){
            log.error("Unexpected error processing ProductDeletedEvent",
                    kv("payload", safePayload(payload)),
                    ex
            );
        }
    }

    private String safePayload(String payload) {
        return payload.length() > 500 ? payload.substring(0, 500) + "..." : payload;
    }

}
