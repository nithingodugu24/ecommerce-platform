package com.nithingodugu.ecommerce.cartservice.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(

        @NotEmpty(message = "Item list cannot be empty")
        @Valid
        List<OrderItemRequest> items
) {
}
