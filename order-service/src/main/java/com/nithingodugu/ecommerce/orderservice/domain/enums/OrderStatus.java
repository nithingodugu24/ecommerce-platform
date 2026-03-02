package com.nithingodugu.ecommerce.orderservice.domain.enums;

public enum OrderStatus {
    CREATED,
    VALIDATED,
    STOCK_RESERVED,
    PAYMENT_AUTHORIZED,
    CONFIRMED,
    REJECTED_INVALID_PRODUCT,
    REJECTED_OUT_OF_STOCK,
    PAYMENT_FAILED,
    CANCELLED

}
