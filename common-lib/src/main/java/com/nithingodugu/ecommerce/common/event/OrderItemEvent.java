package com.nithingodugu.ecommerce.common.event;

import lombok.Data;

@Data
public class OrderItemEvent {

    private Long productId;
    private Integer quantity;
}
