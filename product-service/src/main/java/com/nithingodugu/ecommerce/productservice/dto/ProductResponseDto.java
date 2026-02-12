package com.nithingodugu.ecommerce.productservice.dto;

import java.time.LocalDateTime;

public record ProductResponseDto(
        String name,
        String description,
        Double price,
        String category,
        Boolean active,
        LocalDateTime createdAt
) {
}
