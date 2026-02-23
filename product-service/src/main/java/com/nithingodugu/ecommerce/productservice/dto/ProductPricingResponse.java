package com.nithingodugu.ecommerce.productservice.dto;

import lombok.Data;

public record ProductPricingResponse(
        Long productId,
        String name,
        Double price,
        Boolean active
) {
}
