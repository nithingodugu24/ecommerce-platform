package com.nithingodugu.ecommerce.productservice.event;

import lombok.Data;

@Data
public class ProductCreatedEvent {

    private Long productId;
    private String name;
    private Integer initialQuantity;
}
