package com.nithingodugu.ecommerce.cartservice.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        String productId,
        int quantity,
        BigDecimal price
) {
}
