package com.nithingodugu.ecommerce.orderservice.service;

import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsResponse;
import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.dto.CreateOrderRequest;
import com.nithingodugu.ecommerce.orderservice.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(String userId, CreateOrderRequest request);

    OrderResponse cancelOrder(String userId, String orderId);

    OrderResponse getOrder(String userId, String orderId);

    List<OrderResponse> getMyOrders(String userId);

    OrderDetailsResponse getInternalOrderDetails(String orderId);

    void processPaymentSuccessOrder(String orderId);

    void processPaymentFailedOrder(String orderId);
}
