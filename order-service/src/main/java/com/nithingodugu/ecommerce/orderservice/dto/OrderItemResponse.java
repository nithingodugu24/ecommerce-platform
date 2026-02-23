package com.nithingodugu.ecommerce.orderservice.dto;

public record OrderItemResponse(
        Long productId,
        String productName,
        Double price,
        Integer quantity
) {}