package com.nithingodugu.ecommerce.orderservice.domain.enums;

public enum OrderStatus {
    CREATED,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    COMPLETED,
    CANCELLED
}
