package com.nithingodugu.ecommerce.cartservice.dto;

import lombok.*;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items
) {
}
