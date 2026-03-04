package com.nithingodugu.ecommerce.common.contract.order;

import java.math.BigDecimal;

public record OrderDetailsResponse(
        boolean success,
        String message,
        BigDecimal amount
) {
}
