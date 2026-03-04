package com.nithingodugu.ecommerce.common.contract.payment;

import java.math.BigDecimal;

public record AuthorizePaymentRequest(
        String orderId,
        BigDecimal amount,
        String paymentToken
) {
}
