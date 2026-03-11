package com.nithingodugu.ecommerce.inventoryservice.service;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;
import com.nithingodugu.ecommerce.common.event.ProductDeletedEvent;

public interface InventoryService {

    InventoryReservationResponse reservation(InventoryReservationRequest request);

    void handleProductCreated(ProductCreatedEvent event);

    void handleProductDeleted(ProductDeletedEvent event);
}
