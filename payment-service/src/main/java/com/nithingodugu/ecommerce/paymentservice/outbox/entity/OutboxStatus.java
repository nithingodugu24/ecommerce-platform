package com.nithingodugu.ecommerce.paymentservice.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
