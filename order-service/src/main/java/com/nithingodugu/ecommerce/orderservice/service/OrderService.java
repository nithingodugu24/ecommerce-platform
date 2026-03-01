package com.nithingodugu.ecommerce.orderservice.service;

import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.dto.CreateOrderRequest;
import com.nithingodugu.ecommerce.orderservice.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(UUID userId, CreateOrderRequest request);

    OrderResponse cancelOrder(UUID userId, Long id);

    OrderResponse getOrder(UUID userId, Long id);

    List<OrderResponse> getMyOrders(UUID userId);
}
