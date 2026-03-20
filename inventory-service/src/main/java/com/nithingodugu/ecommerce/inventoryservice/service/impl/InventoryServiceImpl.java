package com.nithingodugu.ecommerce.inventoryservice.service.impl;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationItem;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResult;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
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

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    @Override
    @Transactional
    public InventoryReservationResponse reservation(InventoryReservationRequest request) {

        log.info("Reservation attempt",
                kv("orderId", request.orderId()));

        InventoryReservation reservation = new InventoryReservation();

        for (InventoryReservationItem item : request.items()) {

            int updatedRows = inventoryRepository.reserveStock(
                    item.productId(),
                    item.quantity()
            );

            if (updatedRows == 0) {
                log.warn("Reservation failed",
                        kv("orderId", request.orderId()),
                        kv("reason", "OUT_OF_STOCK_OR_NOT_FOUND"));
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

        log.info("Reservation success",
                kv("orderId", request.orderId()),
                kv("reservationId", reservation.getId()));

        return new InventoryReservationResponse(
                InventoryReservationResult.SUCCESS,
                "Reserved successfully"
        );
    }

    @Override
    @Transactional
    public void handleProductCreated(ProductCreatedEvent event) {

        log.info("Inventory create attempt",
                kv("productId", event.getProductId()),
                kv("quantity", event.getInitialQuantity()));

        if (inventoryRepository.findByProductId(event.getProductId()).isPresent()){

            log.warn("Inventory create failed",
                    kv("productId", event.getProductId()),
                    kv("reason", "DUPLICATE_PRODUCT"));
            throw new DuplicateInventoryException(event.getProductId());
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(event.getProductId());
        inventory.setAvailableQuantity(event.getInitialQuantity());
        inventoryRepository.save(inventory);

        log.info("Inventory created",
                kv("productId", event.getProductId()),
                kv("inventoryId", inventory.getId()));
    }

    @Override
    @Transactional
    public void handleProductDeleted(ProductDeletedEvent event) {

        log.info("Inventory delete (mark inactive) attempt",
                kv("productId", event.getProductId())
        );

        Inventory inventory = inventoryRepository
                .findByProductId(event.getProductId())
                .orElseThrow(() -> {
                    log.warn("Inventory delete failed",
                            kv("productId", event.getProductId()),
                            kv("reason", "NOT_FOUND")
                    );
                    return new InventoryNotFoundException(event.getProductId());
                });

        if (inventory.getStatus() == InventoryStatus.INACTIVE) {
            log.info("Inventory already inactive",
                    kv("productId", event.getProductId())
            );
            return;
        }

        inventory.setStatus(InventoryStatus.INACTIVE);

        log.info("Inventory marked inactive",
                kv("productId", event.getProductId())
        );
    }

    @Override
    @Transactional
    public void processOrderCancelled(OrderCancelledEvent event) {

        log.info("Order cancel processing",
                kv("orderId", event.getOrderId())
        );

        InventoryReservation reservation = inventoryReservationRepository
                .findByOrderId(event.getOrderId())
                .orElseThrow(() -> {
                    log.warn("Order cancel failed",
                            kv("orderId", event.getOrderId()),
                            kv("reason", "RESERVATION_NOT_FOUND")
                    );
                    return new ReservationNotFoundException(event.getOrderId());
                });

        if (reservation.getStatus() == ReservationStatus.RELEASED) {

            log.warn("Duplicate release attempt",
                    kv("orderId", event.getOrderId())
            );

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
                log.error("Inventory missing during release",
                        kv("productId", item.getProductId()),
                        kv("orderId", event.getOrderId())
                );
                continue;
            }

            int updated = inventoryRepository.releaseStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            if (updated == 0) {
                log.error("Stock release failed",
                        kv("productId", item.getProductId()),
                        kv("quantity", item.getQuantity()),
                        kv("orderId", event.getOrderId()),
                        kv("reason", "INSUFFICIENT_RESERVED")
                );
            }
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        inventoryReservationRepository.save(reservation);

        log.info("Order cancel processed",
                kv("orderId", event.getOrderId())
        );
    }

    @Override
    public InventoryResponseDto getInventory(String productId) {

        log.debug("Get inventory",
                kv("productId", productId)
        );

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Inventory fetch failed",
                            kv("productId", productId),
                            kv("reason", "NOT_FOUND")
                    );
                    return new InventoryNotFoundException(productId);
                });

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponseDto updateInventory(String productId, InventoryUpdateRequestDto request){

        log.info("Update inventory attempt",
                kv("productId", productId),
                kv("newQuantity", request.availableQuantity())
        );

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Update inventory failed",
                            kv("productId", productId),
                            kv("reason", "NOT_FOUND")
                    );
                    return new InventoryNotFoundException(productId);
                });

        inventory.setAvailableQuantity(request.availableQuantity());

        log.info("Inventory updated",
                kv("productId", productId),
                kv("availableQuantity", inventory.getAvailableQuantity())
        );

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
