package com.nithingodugu.ecommerce.orderservice.controller;

import com.nithingodugu.ecommerce.orderservice.dto.CreateOrderRequest;
import com.nithingodugu.ecommerce.orderservice.dto.OrderResponse;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@PreAuthorize("isAuthenticated()")
@AllArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateOrderRequest request
            ){
        return ResponseEntity.ok(orderService.createOrder(userId, request));
    }

    @GetMapping
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal String userId){
        return orderService.getMyOrders(userId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal String userId,
            @PathVariable String orderId) {
        return orderService.getOrder(userId, orderId);
    }

    @PostMapping("/cancel/{orderId}")
    public OrderResponse cancelOrder(
            @AuthenticationPrincipal String userId,
            @PathVariable String orderId){
        return orderService.cancelOrder(userId, orderId);
    }
}
