package com.nithingodugu.ecommerce.orderservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationItem;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResult;
import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsResponse;
import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsStatus;
import com.nithingodugu.ecommerce.common.contract.product.*;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.common.event.OrderConfirmedEvent;
import com.nithingodugu.ecommerce.common.event.OrderItemEvent;
import com.nithingodugu.ecommerce.common.exceptions.InvalidProductException;
import com.nithingodugu.ecommerce.common.exceptions.OutOfStockException;
import com.nithingodugu.ecommerce.orderservice.client.InventoryClient;
import com.nithingodugu.ecommerce.orderservice.client.ProductClient;
import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.domain.entity.OrderItem;
import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;
import com.nithingodugu.ecommerce.orderservice.dto.*;
import com.nithingodugu.ecommerce.orderservice.exceptions.DuplicateOrderStateException;
import com.nithingodugu.ecommerce.orderservice.exceptions.OrderNotFoundException;
import com.nithingodugu.ecommerce.orderservice.exceptions.ServiceUnavailableException;
import com.nithingodugu.ecommerce.orderservice.kafka.KafkaTopics;
import com.nithingodugu.ecommerce.orderservice.outbox.entity.OutboxEvent;
import com.nithingodugu.ecommerce.orderservice.outbox.entity.OutboxStatus;
import com.nithingodugu.ecommerce.orderservice.outbox.repository.OutboxEventRepository;
import com.nithingodugu.ecommerce.orderservice.repository.OrderRepository;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import com.nithingodugu.ecommerce.orderservice.util.IdGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.opentelemetry.api.trace.Span;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

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

        log.info("Create order request",
                kv("userId", userId),
                kv("itemsCount", request.items().size())
        );

        long startPricingRequest = System.currentTimeMillis();

        ProductsPricingResponse pricing = getPricing(
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

        log.info("Pricing response received",
                kv("success", pricing.status().name()),
                kv("message", pricing.message()),
                kv("durationMs", System.currentTimeMillis() - startPricingRequest)
        );

        if(pricing.status() == ProductsPricingStatus.INVALID_PRODUCT){
            log.warn("Create order failed - invalid product",
                    kv("reason", pricing.message()));
            throw new InvalidProductException(pricing.message());

        }else if(pricing.status() == ProductsPricingStatus.FAILED){
            throw new ServiceUnavailableException(pricing.message());
        }

        String orderId = idGenerator.generateOrderId();

        long startReservation = System.currentTimeMillis();

        InventoryReservationResponse reservationResponse = reserveInventory(
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

        log.info("Inventory reservation response",
                kv("orderId", orderId),
                kv("status", reservationResponse.status()),
                kv("message", reservationResponse.message()),
                kv("durationMs", System.currentTimeMillis() - startReservation)
        );

        if (reservationResponse.status() == InventoryReservationResult.FAILED){
            throw new ServiceUnavailableException(reservationResponse.message());

        }else if (reservationResponse.status() != InventoryReservationResult.SUCCESS){
            log.warn("Create order failed - out of stock",
                    kv("orderId", orderId),
                    kv("reason", reservationResponse.message()));
            throw new OutOfStockException(reservationResponse.message());
        }

        Order order = setOrderDetails(userId, orderId, pricing);
        order = orderRepository.save(order);

        log.info("Order created successfully",
                kv("orderId", orderId),
                kv("userId", userId),
                kv("amount", order.getTotalAmount())
        );

        return mapToResponse(order);

    }

    @CircuitBreaker(name = "productService", fallbackMethod = "productFallback")
    public ProductsPricingResponse getPricing(ProductsPricingRequest request) {
        return productClient.quote(request);
    }

    public ProductsPricingResponse productFallback(ProductsPricingRequest request, Exception ex) {
        log.error("Product service failed",
                kv("error", ex));

        return new ProductsPricingResponse(
                ProductsPricingStatus.FAILED,
                "Product service unavailable",
                List.of(),
                null
        );
    }

    public InventoryReservationResponse inventoryFallback(InventoryReservationRequest request, Exception ex) {
        log.error("Inventory service failed", kv("error", ex));

        return new InventoryReservationResponse(
                InventoryReservationResult.FAILED,
                "Inventory service unavailable"
        );
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "inventoryFallback")
    public InventoryReservationResponse reserveInventory(InventoryReservationRequest request) {
        return inventoryClient.reservation(request);
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
        order.setOrderStatus(OrderStatus.PENDING);
        return order;
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String userId, String orderId) {

        log.info("Cancel order request",
                kv("orderId", orderId),
                kv("userId", userId)
        );

        Order order = orderRepository
                .findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Cancel order failed - not found",
                            kv("orderId", orderId));
                    return new OrderNotFoundException("Order id not found");
                });

        if (order.getOrderStatus() == OrderStatus.CANCELLED){
            log.warn("Cancel order ignored - already cancelled",
                    kv("orderId", orderId));
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
            log.error("Failed to serialize OrderCancelledEvent", kv("error", ex.getMessage()));
            throw new RuntimeException("Failed to serialize OrderCancelledEvent", ex);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setAggregateId(orderId);
        outbox.setTopic(KafkaTopics.ORDER_CANCELLED);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);

        outbox.setRequestId(MDC.get("requestId"));

        var currentSpan = Span.current().getSpanContext();
        if (currentSpan.isValid()) {
            outbox.setOriginalTraceId(currentSpan.getTraceId());
            outbox.setOriginalSpanId(currentSpan.getSpanId());
        }

        outboxEventRepository.save(outbox);

        log.info("Order cancelled successfully",
                kv("orderId", orderId)
        );

        return mapToResponse(order);
    }

    @Override
    public OrderResponse getOrder(String userId, String orderId) {

        log.debug("GetOrder request",
                kv("userId", userId),
                kv("orderId", orderId));

        Order order = orderRepository
                .findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("GetOrder failed",
                            kv("userId", userId),
                            kv("orderId", orderId));
                    return new OrderNotFoundException("Order not found");
                });

        log.debug("GetOrder request completed",
                kv("userId", userId),
                kv("orderId", orderId));

        return mapToResponse(order);
    }

    @Override
    public List<OrderResponse> getMyOrders(String userId) {

        log.debug("MyOrders request",
                kv("userId", userId));

        List<Order> orders = orderRepository.findByUserId(userId);

        log.debug("MyOrders request completed",
                kv("userId", userId));

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

        log.info("Internal OrderDetails request",
                kv("orderId", orderId));

        Order order = orderRepository.findByOrderId(orderId).orElse(null);

        if (order == null){
            log.warn("Internal OrderDetails failed",
                    kv("reason", "INVALID_ORDER"),
                    kv("orderId", orderId));
            return new OrderDetailsResponse(
                    OrderDetailsStatus.INVALID,
                    "Invalid Order",
                    null
            );
        }

        log.info("Internal OrderDetails sucess",
                kv("orderId", orderId));

        return new OrderDetailsResponse(
                OrderDetailsStatus.SUCCESS,
                "success",
                order.getTotalAmount()
        );
    }

    @Override
    @Transactional
    public void processPaymentSuccessOrder(String orderId) {

        log.info("Process payment success",
                kv("orderId", orderId)
        );

        Order order = orderRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("Process Payment success is failed ",
                            kv("reason", "ORDER_NOT_FOUND"),
                            kv("orderId", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getOrderStatus() == OrderStatus.CONFIRMED){
            log.warn("Duplicate payment success event",
                    kv("orderId", orderId));
            throw new DuplicateOrderStateException(orderId);
        }

        order.setOrderStatus(OrderStatus.CONFIRMED);

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                orderId
        );

        String payload = "";

        try {
            payload = objectMapper.writeValueAsString(event);

        }catch (JsonProcessingException ex){
            log.error("Serialization failed for OrderConfirmedEvent",
                    kv("error", ex.getMessage())
            );
            throw new RuntimeException("Failed to serialize OrderConfirmedEvent", ex);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setAggregateId(orderId);
        outbox.setTopic(KafkaTopics.ORDER_CONFIRMED);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);

        outbox.setRequestId(MDC.get("requestId"));

        var currentSpan = Span.current().getSpanContext();
        if (currentSpan.isValid()) {
            outbox.setOriginalTraceId(currentSpan.getTraceId());
            outbox.setOriginalSpanId(currentSpan.getSpanId());
        }

        log.info("Payment success processed",
                kv("orderId", orderId)
        );

        outboxEventRepository.save(outbox);
    }

    @Override
    @Transactional
    public void processPaymentFailedOrder(String orderId) {

        log.info("Process payment failed",
                kv("orderId", orderId)
        );

        Order order = orderRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("Process Payment failed error ",
                            kv("reason", "ORDER_NOT_FOUND"),
                            kv("orderId", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getOrderStatus() == OrderStatus.FAILED){
            log.warn("Duplicate payment failed event",
                    kv("orderId", orderId));
            throw new DuplicateOrderStateException(orderId);
        }

        order.setOrderStatus(OrderStatus.FAILED);

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

        String payload = "";

        try {
            payload = objectMapper.writeValueAsString(event);

        }catch (JsonProcessingException ex){
            log.error("Serialization failed for OrderCancelledEvent",
                    kv("error", ex.getMessage())
            );
            throw new RuntimeException("Failed to serialize OrderCancelledEvent", ex);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setAggregateId(orderId);
        outbox.setTopic(KafkaTopics.ORDER_FAILED);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);

        outbox.setRequestId(MDC.get("requestId"));

        var currentSpan = Span.current().getSpanContext();
        if (currentSpan.isValid()) {
            outbox.setOriginalTraceId(currentSpan.getTraceId());
            outbox.setOriginalSpanId(currentSpan.getSpanId());
        }
        outboxEventRepository.save(outbox);

        log.info("Payment failed processed",
                kv("orderId", orderId)
        );
    }
}
