package com.nithingodugu.ecommerce.inventoryservice.kafka;

import com.nithingodugu.ecommerce.common.event.*;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.created")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event){

        log.info("New order recieved");

        List<OrderItemEvent> items = event.getItems();
        for(OrderItemEvent item: items){
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).orElseThrow();

            if (inventory.getAvailableQuantity() - inventory.getReservedQuantity() >= item.getQuantity()){

                inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
                inventoryRepository.save(inventory);
                log.info("{} reserved", item.getProductId());
            }else{
                //send event to cancel the order as product went outofstock
                log.info("failed event");
                InventoryFailedEvent failedEvent = new InventoryFailedEvent();
                failedEvent.setOrderId(event.getOrderId());
                kafkaTemplate.send("inventory.failed", failedEvent);
                return;
            }
        }

        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                event.getOrderId()
        );

        kafkaTemplate.send("inventory.reserved", reservedEvent);
        log.info("order inventory reserved successfully");

    }

    @KafkaListener(topics = {"order.cancelled"})
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event){

        List<OrderItemEvent> items = event.getItems();
        for(OrderItemEvent item: items){
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).orElseThrow();
            inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);
        }

        log.info("released all products from reserved !");

    }
}
