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
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;

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
            inventoryService.handleProductCreated(event);
            ack.acknowledge();

        }catch (JsonProcessingException ex) {
            log.error("Failed to deserialize ProductCreatedEvent payload={}", payload, ex);

        }catch (DuplicateInventoryException ex) {
            log.error("Inventory already exists with productId={}", ex.getMessage());
            ack.acknowledge();

        }catch (Exception ex){
            log.error("Failed to handle ProductCreatedEvent payload={}", payload, ex);
        }
    }

    @KafkaListener(topics = "product.deleted")
    public void handleProductDeleted(
            @Payload String payload,
            Acknowledgment ack
    ){
        try {
            ProductDeletedEvent event = objectMapper.readValue(payload, ProductDeletedEvent.class);
            inventoryService.handleProductDeleted(event);
            ack.acknowledge();

        }catch (JsonProcessingException ex) {
            log.error("Failed to deserialize ProductDeletedEvent payload={}", payload, ex);

        }catch (InventoryNotFoundException ex) {
            log.error("Inventory not found with productId={}", ex.getMessage());
            ack.acknowledge();

        }catch (Exception ex){
            log.error("Failed to handle ProductDeletedEvent payload={}", payload, ex);
        }
    }
}
