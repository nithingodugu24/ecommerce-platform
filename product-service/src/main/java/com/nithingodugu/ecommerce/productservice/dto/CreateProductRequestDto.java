package com.nithingodugu.ecommerce.productservice.dto;

public record CreateProductRequestDto(
        String name,
        String description,
        Double price,
        String category,
        Integer availableQuantity
) {
}
