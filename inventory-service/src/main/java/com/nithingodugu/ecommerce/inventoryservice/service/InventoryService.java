package com.nithingodugu.ecommerce.inventoryservice.service;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;
import com.nithingodugu.ecommerce.common.event.ProductDeletedEvent;
import com.nithingodugu.ecommerce.inventoryservice.dto.InventoryResponseDto;
import com.nithingodugu.ecommerce.inventoryservice.dto.InventoryUpdateRequestDto;
import jakarta.transaction.Transactional;

public interface InventoryService {

    InventoryReservationResponse reservation(InventoryReservationRequest request);

    void handleProductCreated(ProductCreatedEvent event);

    void handleProductDeleted(ProductDeletedEvent event);

    InventoryResponseDto getInventory(String productId);

    InventoryResponseDto updateInventory(String productId, InventoryUpdateRequestDto request);

    void processOrderCancelled(OrderCancelledEvent event);
}
