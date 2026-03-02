package com.nithingodugu.ecommerce.inventoryservice.controller.internal;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/inventory/reservations")
@RequiredArgsConstructor
public class InventoryReservationsController {

    private final InventoryService inventoryService;

    @PostMapping("")
    public InventoryReservationResponse reservation(@RequestBody InventoryReservationRequest request){
        return inventoryService.reservation(request);
    }

}
