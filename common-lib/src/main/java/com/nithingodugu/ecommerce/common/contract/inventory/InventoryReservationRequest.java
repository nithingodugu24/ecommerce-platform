package com.nithingodugu.ecommerce.common.contract.inventory;

import java.util.List;

public record InventoryReservationRequest(
        String orderId,
        List<InventoryReservationItem> items
) {
}
