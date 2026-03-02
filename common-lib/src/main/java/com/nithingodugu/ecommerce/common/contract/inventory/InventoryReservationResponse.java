package com.nithingodugu.ecommerce.common.contract.inventory;

public record InventoryReservationResponse(
        InventoryReservationResult status,
        String message
) {
}
