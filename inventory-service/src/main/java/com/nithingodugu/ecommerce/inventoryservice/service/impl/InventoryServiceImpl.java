package com.nithingodugu.ecommerce.inventoryservice.service.impl;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationItem;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResult;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.common.event.OrderItemEvent;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;
import com.nithingodugu.ecommerce.common.event.ProductDeletedEvent;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.InventoryReservation;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.ReservationItem;
import com.nithingodugu.ecommerce.inventoryservice.domain.enums.InventoryStatus;
import com.nithingodugu.ecommerce.inventoryservice.domain.enums.ReservationStatus;
import com.nithingodugu.ecommerce.inventoryservice.dto.InventoryResponseDto;
import com.nithingodugu.ecommerce.inventoryservice.dto.InventoryUpdateRequestDto;
import com.nithingodugu.ecommerce.inventoryservice.exceptions.*;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryRepository;
import com.nithingodugu.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.nithingodugu.ecommerce.inventoryservice.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    @Override
    @Transactional
    public InventoryReservationResponse reservation(InventoryReservationRequest request) {

        InventoryReservation reservation = new InventoryReservation();

        for (InventoryReservationItem item : request.items()) {

            int updatedRows = inventoryRepository.reserveStock(
                    item.productId(),
                    item.quantity()
            );

            if (updatedRows == 0) {
                log.info("product outofstock");
                return new InventoryReservationResponse(
                        InventoryReservationResult.OUT_OF_STOCK,
                        "product out of stock or not found"
                );
            }

            reservation.addItem(
                    item.productId(),
                    item.quantity()
            );
        }

        reservation.setOrderId(request.orderId());
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        inventoryReservationRepository.save(reservation);

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

    @Override
    @Transactional
    public void processOrderCancelled(OrderCancelledEvent event){

        InventoryReservation reservation = inventoryReservationRepository
                .findByOrderId(event.getOrderId())
                .orElseThrow(() -> new ReservationNotFoundException(event.getOrderId()));

        if (reservation.getStatus() == ReservationStatus.RELEASED){
            throw new DuplicateReleaseException(event.getOrderId());
        }

        List<String> productIds = reservation.getItems().stream()
                .map(ReservationItem::getProductId)
                .toList();

        Map<String, Inventory> inventoryMap = inventoryRepository
                .findAllByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        for (ReservationItem item : reservation.getItems()) {
            Inventory inventory = inventoryMap.get(item.getProductId());

            if (inventory == null) {
                log.error("Inventory not found for productId: {}", item.getProductId());
//                throw new InventoryNotFoundException(item.getProductId());
            }

            int updated = inventoryRepository.releaseStock(item.getProductId(), item.getQuantity());

            if (updated == 0) {
                log.error("Failed to release stock for productId: {} — reservedQty may be insufficient",
                        item.getProductId());
//                throw new InventoryReleaseException(event.getOrderId());
            }
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        inventoryReservationRepository.save(reservation);

    }


    @Override
    public InventoryResponseDto getInventory(String productId) {
        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(()-> new InventoryNotFoundException(productId));

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponseDto updateInventory(String productId, InventoryUpdateRequestDto request){
        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(()-> new InventoryNotFoundException(productId));

        inventory.setAvailableQuantity(request.availableQuantity());

        return mapToResponse(inventory);
    }

    private InventoryResponseDto mapToResponse(Inventory inventory){
        return new InventoryResponseDto(
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getStatus()
        );
    }
}
