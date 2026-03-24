package com.nithingodugu.ecommerce.common.contract.order;

import java.math.BigDecimal;

public record OrderDetailsResponse(
        OrderDetailsStatus status,
        String message,
        BigDecimal amount
) {
}
