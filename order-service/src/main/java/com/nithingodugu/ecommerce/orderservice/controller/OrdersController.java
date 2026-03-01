package com.nithingodugu.ecommerce.orderservice.controller;

import com.nithingodugu.ecommerce.orderservice.dto.CreateOrderRequest;
import com.nithingodugu.ecommerce.orderservice.dto.OrderResponse;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(name = "X-USER-ID") UUID userId,
            @RequestBody CreateOrderRequest request
            ){
        return ResponseEntity.ok(orderService.createOrder(userId, request));
    }

    @GetMapping
    public List<OrderResponse> getMyOrders(@RequestHeader(name = "X-USER-ID") UUID userId){
        return orderService.getMyOrders(userId);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @RequestHeader(name = "X-USER-ID") UUID userId,
            @PathVariable Long id) {
        return orderService.getOrder(userId, id);
    }

    @PostMapping("/cancel/{id}")
    public OrderResponse cancelOrder(
            @RequestHeader(name = "X-USER-ID") UUID userId,
            @PathVariable Long id){
        return orderService.cancelOrder(userId, id);
    }
}
