package com.nithingodugu.ecommerce.inventoryservice.kafka;

import com.nithingodugu.ecommerce.common.event.*;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryRepository inventoryRepository;


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
