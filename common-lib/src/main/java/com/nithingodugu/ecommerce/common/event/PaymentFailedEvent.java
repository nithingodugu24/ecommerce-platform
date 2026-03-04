package com.nithingodugu.ecommerce.common.event;

public record PaymentFailedEvent(
        String orderId,
        String errorCode
) {
}
