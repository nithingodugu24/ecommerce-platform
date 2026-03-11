package com.nithingodugu.ecommerce.inventoryservice.dto;

import com.nithingodugu.ecommerce.inventoryservice.domain.enums.InventoryStatus;

public record InventoryResponseDto(
        String productId,
        Integer availableQuantity,
        Integer reservedQuantity,
        InventoryStatus status
) {
}
