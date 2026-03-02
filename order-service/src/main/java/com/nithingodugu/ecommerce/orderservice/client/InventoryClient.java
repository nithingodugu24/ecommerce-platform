package com.nithingodugu.ecommerce.orderservice.client;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("inventory-service")
public interface InventoryClient {

    @PostMapping("/internal/inventory/reservations")
    InventoryReservationResponse reservation(@RequestBody InventoryReservationRequest request);
}
