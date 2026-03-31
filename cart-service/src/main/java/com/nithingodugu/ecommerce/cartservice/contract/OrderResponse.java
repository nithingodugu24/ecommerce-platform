package com.nithingodugu.ecommerce.cartservice.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


public record OrderResponse(
        String orderId,
        String status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt

) {
}
