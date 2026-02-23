package com.nithingodugu.ecommerce.orderservice.controller;

import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.dto.CreateOrderRequest;
import com.nithingodugu.ecommerce.orderservice.dto.OrderResponse;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import jakarta.persistence.PostRemove;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request
            ){
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

}
