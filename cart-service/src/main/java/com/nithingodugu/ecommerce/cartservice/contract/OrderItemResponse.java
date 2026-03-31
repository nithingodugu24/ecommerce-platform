package com.nithingodugu.ecommerce.cartservice.contract;

public record OrderItemResponse(
        String productId,
        String productName,
        Double price,
        Integer quantity
) {}