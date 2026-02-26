package com.nithingodugu.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private UUID userId;

    private Long orderId;

    private BigDecimal totalAmount;

    private List<OrderItemEvent> items;
}
