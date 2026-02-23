package com.nithingodugu.ecommerce.orderservice.dto;

import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime createdAt

) {
}
