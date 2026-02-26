package com.nithingodugu.ecommerce.common.event;

import lombok.Data;

@Data
public class ProductCreatedEvent {

    private Long productId;
    private String name;
    private Integer initialQuantity;
}

