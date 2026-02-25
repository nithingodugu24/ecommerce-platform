package com.nithingodugu.ecommerce.inventoryservice.kafka;

import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import com.nithingodugu.ecommerce.inventoryservice.event.ProductCreatedEvent;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ProductEventListener {

    private final InventoryRepository inventoryRepository;

    @KafkaListener(topics = "product.created", groupId = "inventory-group")
    @Transactional
    public void handleProductCreated(ProductCreatedEvent event){

        if(inventoryRepository.findByProductId(event.getProductId()).isPresent()){
            log.info("product already created");
            return;
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(event.getProductId());
        inventory.setAvailableQuantity(event.getInitialQuantity());

        inventoryRepository.save(inventory);

        log.info("Product created");
    }
}
