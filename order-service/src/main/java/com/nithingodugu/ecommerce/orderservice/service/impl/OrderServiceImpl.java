package com.nithingodugu.ecommerce.orderservice.service.impl;

import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.common.event.OrderCreatedEvent;
import com.nithingodugu.ecommerce.common.event.OrderItemEvent;
import com.nithingodugu.ecommerce.orderservice.client.product.ProductClient;
import com.nithingodugu.ecommerce.orderservice.config.KafkaConfig;
import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.domain.entity.OrderItem;
import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;
import com.nithingodugu.ecommerce.orderservice.dto.*;
import com.nithingodugu.ecommerce.orderservice.repository.OrderRepository;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {

        List<Long> productIds = request.items()
                .stream()
                .map(OrderItemRequest::productId)
                .toList();

        if(productIds.isEmpty()){
            throw new IllegalArgumentException("Invalid Order");
        }

        log.debug("Got product ids starting conection to client");

        List<ProductPricingResponse> pricing = productClient.getBulkProductPricing(productIds);

        log.debug("got response");



        if(pricing == null || pricing.isEmpty()){
            throw new IllegalArgumentException("Invalid Order");
        }

        Order order = new Order();
        order.setUserId(request.userId());
        order.setOrderStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;

        for(OrderItemRequest itemRequest: request.items()){

            ProductPricingResponse product = pricing.stream()
                    .filter(p -> p.productId().equals(itemRequest.productId()))
                    .findFirst()
                    .orElseThrow();

            if (itemRequest.quantity() <= 0 || !product.active()){
                throw new RuntimeException("Product inactive");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.productId());
            item.setProductName(product.name());
            item.setPrice(product.price());
            item.setQuantity(itemRequest.quantity());

            total = total.add(
                    BigDecimal.valueOf(product.price()).
                            multiply(BigDecimal.valueOf(itemRequest.quantity()))
            );

            order.getOrderItems().add(item);

        }

        order.setTotalAmount(total);
        orderRepository.save(order);

        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity()
                ))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setTotalAmount(order.getTotalAmount());
        event.setUserId(order.getUserId());

        List<OrderItemEvent> eventItems = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemEvent(
                                item.getProductId(),
                                item.getQuantity()
                        ))
                .toList();
        event.setItems(eventItems);

        kafkaTemplate.send("order.created", event);


        return new OrderResponse(
                order.getId(),
                order.getOrderStatus().name(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );

    }

    @Override
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow();
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);



        List<OrderItemEvent> eventItems = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(order.getId());
        event.setItems(eventItems);
        kafkaTemplate.send("order.cancelled", event);

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
                order.getId(),
                order.getOrderStatus().name(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );

    }

    @Override
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow();

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
                order.getId(),
                order.getOrderStatus().name(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );

    }

    @Override
    public List<OrderResponse> getMyOrders(UUID userId) {
        List<Order> orders = orderRepository.findByUserId(userId);

        return orders.stream()
                .map(order -> new OrderResponse(
                        order.getId(),
                        order.getOrderStatus().name(),
                        order.getTotalAmount(),
                        order.getOrderItems().stream().map(orderItem -> new OrderItemResponse(
                                orderItem.getProductId(),
                                orderItem.getProductName(),
                                orderItem.getPrice(),
                                orderItem.getQuantity()
                        )).toList(),
                        order.getCreatedAt()
                ))
                .toList();

    }
}
