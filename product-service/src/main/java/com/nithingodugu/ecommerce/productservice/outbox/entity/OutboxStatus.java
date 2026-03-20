package com.nithingodugu.ecommerce.productservice.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
