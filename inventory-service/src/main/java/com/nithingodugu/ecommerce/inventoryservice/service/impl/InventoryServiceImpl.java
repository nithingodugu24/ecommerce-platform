package com.nithingodugu.ecommerce.inventoryservice.service.impl;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationItem;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResult;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.InventoryReservation;
import com.nithingodugu.ecommerce.inventoryservice.domain.entity.enums.ReservationStatus;
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
                return new InventoryReservationResponse(
                        InventoryReservationResult.OUT_OF_STOCK,
                        "product out of stock or not found"
                );
            }
        }

        InventoryReservation inventoryReservation = new InventoryReservation();
        inventoryReservation.setOrderId(Long.valueOf(request.orderId()));
        inventoryReservation.setStatus(ReservationStatus.RESERVED);
        inventoryReservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        inventoryReservationRepository.save(inventoryReservation);

        return new InventoryReservationResponse(
                InventoryReservationResult.SUCCESS,
                "Reserved successfully"
        );
    }
}
