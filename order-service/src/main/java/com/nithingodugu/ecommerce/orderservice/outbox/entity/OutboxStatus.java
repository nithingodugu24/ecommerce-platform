package com.nithingodugu.ecommerce.orderservice.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
