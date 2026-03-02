package com.nithingodugu.ecommerce.inventoryservice.service;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;

public interface InventoryService {

    InventoryReservationResponse reservation(InventoryReservationRequest request);
}
