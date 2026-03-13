package com.nithingodugu.ecommerce.orderservice.dto;

public record OrderItemResponse(
        String productId,
        String productName,
        Double price,
        Integer quantity
) {}