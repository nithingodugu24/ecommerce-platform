package com.nithingodugu.ecommerce.cartservice.controller;

import com.nithingodugu.ecommerce.cartservice.contract.OrderResponse;
import com.nithingodugu.ecommerce.cartservice.dto.AddToCartRequest;
import com.nithingodugu.ecommerce.cartservice.dto.CartResponse;
import com.nithingodugu.ecommerce.cartservice.dto.UpdateCartItemRequest;
import com.nithingodugu.ecommerce.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/cart")
@RestController
public class AuthUserCartController {

    private final CartService cartService;

    @GetMapping()
    public CartResponse getCart(
            @AuthenticationPrincipal String userId
    ){
        return cartService.getCart(userId);
    }

    @PostMapping()
    public CartResponse addToCart(
            @AuthenticationPrincipal String userId,
            @RequestBody AddToCartRequest request

            ){
        return cartService.addToCart(userId, request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItemQuantity(
            @AuthenticationPrincipal String userId,
            @PathVariable String productId,
            @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItemQuantity(userId, productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @AuthenticationPrincipal String userId,
            @PathVariable String productId
    ) {
        return cartService.removeItem(userId, productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus  .NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal String userId) {
        cartService.clearCart(userId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal String userId) {
        return cartService.checkout(userId);
    }



}
