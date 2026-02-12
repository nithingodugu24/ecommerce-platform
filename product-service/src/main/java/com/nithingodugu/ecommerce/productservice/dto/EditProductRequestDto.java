package com.nithingodugu.ecommerce.productservice.dto;

public record EditProductRequestDto(
            String name,
            String description,
            Double price,
            String category,
            Boolean active
){}