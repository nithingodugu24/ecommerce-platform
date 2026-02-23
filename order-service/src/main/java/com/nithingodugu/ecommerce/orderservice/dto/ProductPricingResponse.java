package com.nithingodugu.ecommerce.orderservice.dto;

public record ProductPricingResponse(
        Long productId,
        String name,
        Double price,
        Boolean active
) {
}
