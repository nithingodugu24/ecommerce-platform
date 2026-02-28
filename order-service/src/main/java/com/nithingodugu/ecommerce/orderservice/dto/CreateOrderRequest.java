package com.nithingodugu.ecommerce.orderservice.dto;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        List<OrderItemRequest> items
) {
}
