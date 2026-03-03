package com.nithingodugu.ecommerce.paymentservice.dto;

public record PaymentWebhookRequest(
        String paymentId,
        Boolean success,
        String referenceId,
        String errorCode
) {
}
