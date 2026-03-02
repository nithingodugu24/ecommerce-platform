package com.nithingodugu.ecommerce.orderservice.controller;

import com.nithingodugu.ecommerce.orderservice.dto.CreateOrderRequest;
import com.nithingodugu.ecommerce.orderservice.dto.OrderResponse;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
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
            @AuthenticationPrincipal UUID userId,
            @RequestBody CreateOrderRequest request
            ){
        return ResponseEntity.ok(orderService.createOrder(userId, request));
    }

    @GetMapping
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal UUID userId){
        return orderService.getMyOrders(userId);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable Long id) {
        return orderService.getOrder(userId, id);
    }

    @PostMapping("/cancel/{id}")
    public OrderResponse cancelOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable Long id){
        return orderService.cancelOrder(userId, id);
    }
}
