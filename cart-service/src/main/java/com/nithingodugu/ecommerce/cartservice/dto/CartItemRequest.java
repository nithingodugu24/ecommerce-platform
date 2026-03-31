package com.nithingodugu.ecommerce.cartservice.dto;

public record CartItemRequest(
        String productId,
        int quantity
) {
}
