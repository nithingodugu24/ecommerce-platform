package com.nithingodugu.ecommerce.common.event;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreatedEvent {
    private Long userId;

    private Long orderId;

    private BigDecimal totalAmount;

    private List<OrderItemEvent> items;
}
