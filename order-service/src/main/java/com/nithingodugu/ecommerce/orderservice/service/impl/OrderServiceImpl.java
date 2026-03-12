package com.nithingodugu.ecommerce.orderservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationItem;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResult;
import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsResponse;
import com.nithingodugu.ecommerce.common.contract.product.ProductPriceDetail;
import com.nithingodugu.ecommerce.common.contract.product.ProductPricingItem;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingRequest;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingResponse;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.common.event.OrderItemEvent;
import com.nithingodugu.ecommerce.common.exceptions.InvalidProductException;
import com.nithingodugu.ecommerce.common.exceptions.OrderNotFoundException;
import com.nithingodugu.ecommerce.common.exceptions.OutOfStockException;
import com.nithingodugu.ecommerce.orderservice.client.InventoryClient;
import com.nithingodugu.ecommerce.orderservice.client.ProductClient;
import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.domain.entity.OrderItem;
import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;
import com.nithingodugu.ecommerce.orderservice.dto.*;
import com.nithingodugu.ecommerce.orderservice.kafka.KafkaTopics;
import com.nithingodugu.ecommerce.orderservice.outbox.entity.OutboxEvent;
import com.nithingodugu.ecommerce.orderservice.outbox.entity.OutboxStatus;
import com.nithingodugu.ecommerce.orderservice.outbox.repository.OutboxEventRepository;
import com.nithingodugu.ecommerce.orderservice.repository.OrderRepository;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import com.nithingodugu.ecommerce.orderservice.util.IdGenerator;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;

    @Override
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {

         //Call product internal service to validate and calculate totalAmount

        ProductsPricingResponse pricing = productClient.quote(
                new ProductsPricingRequest(request.items().stream()
                        .map(item->
                                new ProductPricingItem(
                                        item.productId(),
                                        item.quantity()
                                )
                        )
                        .toList()
                )
        );

        log.info("got response {} {}",pricing.message(), pricing.success());

        if(!pricing.success()){
            throw new InvalidProductException(pricing.message());
        }

        String orderId = idGenerator.generateOrderId();

        InventoryReservationResponse reservationResponse = inventoryClient.reservation(
                new InventoryReservationRequest(
                        orderId,
                        request.items()
                                .stream()
                                .map(item->
                                        new InventoryReservationItem(
                                                item.productId(),
                                                item.quantity()
                                        )
                                )
                                .toList()

                )
        );

        log.info(reservationResponse.message());
        log.info(String.valueOf(reservationResponse.status()));

        if (reservationResponse.status() != InventoryReservationResult.SUCCESS){
            throw new OutOfStockException(reservationResponse.message());
        }

        Order order = setOrderDetails(userId, orderId, pricing);
        order = orderRepository.save(order);

        return mapToResponse(order);

    }

    private static @NonNull Order setOrderDetails(String userId, String orderId, ProductsPricingResponse pricing) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(pricing.totalAmount());

        for(ProductPriceDetail product: pricing.items()){

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.productId());
            item.setProductName(product.name());
            item.setPrice(product.unitPrice().doubleValue());
            item.setQuantity(product.quantity());

            order.addOrderItem(item);

        }
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        return order;
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String userId, String orderId) {

        Order order = orderRepository
                .findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order id not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED){
            throw new IllegalStateException("Order already cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        List<OrderItemEvent> eventItems = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(order.getOrderId());
        event.setAmountPaid(order.getTotalAmount());
        event.setItems(eventItems);

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);

        }catch (JsonProcessingException ex){
            throw new RuntimeException("Failed to serialize OrderCancelledEvent", ex);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setAggregateId(orderId);
        outbox.setTopic(KafkaTopics.ORDER_CANCELLED);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(outbox);

        return mapToResponse(order);
    }

    @Override
    public OrderResponse getOrder(String userId, String orderId) {

        Order order = orderRepository
                .findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        return mapToResponse(order);
    }

    @Override
    public List<OrderResponse> getMyOrders(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);

        return orders.stream()
                .map(this::mapToResponse)
                .toList();

    }

    private OrderResponse mapToResponse(Order order){

        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity()
                ))
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getOrderStatus().name(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );
    }

    @Override
    public OrderDetailsResponse getInternalOrderDetails(String orderId) {

        Order order = orderRepository.findByOrderId(orderId).orElse(null);

        if (order == null){
            return new OrderDetailsResponse(
                    false,
                    "Invalid Order",
                    null
            );
        }

        return new OrderDetailsResponse(
                true,
                "success",
                order.getTotalAmount()
        );
    }
}
