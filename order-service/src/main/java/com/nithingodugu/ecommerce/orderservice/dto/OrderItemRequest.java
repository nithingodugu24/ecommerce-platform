package com.nithingodugu.ecommerce.orderservice.dto;

public record OrderItemRequest(
        Long productId,
        Integer quantity
) {
}
