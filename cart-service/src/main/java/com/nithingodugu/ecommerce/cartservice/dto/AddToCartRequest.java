package com.nithingodugu.ecommerce.cartservice.dto;

import java.util.List;

public record AddToCartRequest(
    List<CartItemRequest> items
) {
}
