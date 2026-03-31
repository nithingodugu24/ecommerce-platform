package com.nithingodugu.ecommerce.cartservice.contract;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Product id cannot be null")
        String productId,

        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "quantity minimum should be 1")
        Integer quantity
) {
}
