package com.nithingodugu.ecommerce.orderservice.controller;

import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsResponse;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/orders")
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public OrderDetailsResponse orderDetails(@PathVariable String orderId){
        return orderService.getInternalOrderDetails(orderId);
    }
}
