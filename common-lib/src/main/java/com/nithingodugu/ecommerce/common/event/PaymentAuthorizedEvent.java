package com.nithingodugu.ecommerce.common.event;

public record PaymentAuthorizedEvent(
        String orderId
) {
}
