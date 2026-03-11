package com.nithingodugu.ecommerce.inventoryservice.service.impl;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationItem;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResult;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;
import com.nithingodugu.ecommerce.common.event.ProductDeletedEvent;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.InventoryReservation;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.enums.InventoryStatus;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.enums.ReservationStatus;
import com.nithingodugu.ecommerce.inventoryservice.exceptions.DuplicateInventoryException;
import com.nithingodugu.ecommerce.inventoryservice.exceptions.InventoryNotFoundException;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryRepository;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.nithingodugu.ecommerce.inventoryservice.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    @Transactional
    @Override
    public InventoryReservationResponse reservation(InventoryReservationRequest request) {

        String reservationId = UUID.randomUUID().toString();

        for (InventoryReservationItem item : request.items()) {

            int updatedRows = inventoryRepository.reserveStock(
                    item.productId(),
                    item.quantity()
            );

            log.info("upaded rows {}", updatedRows);

            if (updatedRows == 0) {
                log.info("product outofstock");
                return new InventoryReservationResponse(
                        InventoryReservationResult.OUT_OF_STOCK,
                        "product out of stock or not found"
                );
            }
        }

        InventoryReservation inventoryReservation = new InventoryReservation();
        inventoryReservation.setOrderId(request.orderId());
        inventoryReservation.setStatus(ReservationStatus.RESERVED);
        inventoryReservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        inventoryReservationRepository.save(inventoryReservation);

        return new InventoryReservationResponse(
                InventoryReservationResult.SUCCESS,
                "Reserved successfully"
        );
    }

    @Override
    @Transactional
    public void handleProductCreated(ProductCreatedEvent event) {

        if (inventoryRepository.findByProductId(event.getProductId()).isPresent()){
            throw new DuplicateInventoryException(event.getProductId());
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(event.getProductId());
        inventory.setAvailableQuantity(event.getInitialQuantity());
        inventoryRepository.save(inventory);

        log.info("Inventory created for productId={}", event.getProductId());
    }

    @Override
    @Transactional
    public void handleProductDeleted(ProductDeletedEvent event) {

        Inventory inventory = inventoryRepository
                .findByProductId(event.getProductId())
                .orElseThrow(()-> new InventoryNotFoundException(event.getProductId()));

        if (inventory.getStatus() == InventoryStatus.INACTIVE) return;

        inventory.setStatus(InventoryStatus.INACTIVE);

        log.info("Marking inventory Inactive for productId={}", event.getProductId());
    }
}
