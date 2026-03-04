package com.nithingodugu.ecommerce.paymentservice.dto;

public record PaymentCreateResponse(
        String orderId,
        String paymentId,
        String gatewayUrl
) {
}
