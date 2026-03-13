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


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = {"order.cancelled","order.failed"})
    @Transactional
    public void handleOrderCancelled(
            @Payload String payload,
            Acknowledgment ack

    ){

        try {
            OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
            inventoryService.processOrderCancelled(event);
            ack.acknowledge();
        }catch (JsonProcessingException ex) {
            log.error("Failed to deserialize PaymentAuthorizedEvent payload={}", payload, ex);

        }catch (ReservationNotFoundException ex){
            log.error("Reservation not exist with OrderId=,{}", ex);
            ack.acknowledge();
        }catch (DuplicateReleaseException ex){
            log.error("Reservation already released for OrderId=,{}", ex);
            ack.acknowledge();
        }catch (Exception ex){
            log.error("Failed to handle OrderCancelledEvent payload={}", payload, ex);
        }

    }
}
