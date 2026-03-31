package com.nithingodugu.ecommerce.cartservice.service;

import com.nithingodugu.ecommerce.cartservice.contract.OrderResponse;
import com.nithingodugu.ecommerce.cartservice.dto.AddToCartRequest;
import com.nithingodugu.ecommerce.cartservice.dto.CartResponse;
import org.springframework.stereotype.Service;

public interface CartService {

    CartResponse getCart(String userId);

    CartResponse addToCart(String userId, AddToCartRequest request);

    CartResponse updateItemQuantity(String userId, String productId, int quantity);

    CartResponse removeItem(String userId, String productId);

    void clearCart(String userId);

    OrderResponse checkout(String userId);
}
