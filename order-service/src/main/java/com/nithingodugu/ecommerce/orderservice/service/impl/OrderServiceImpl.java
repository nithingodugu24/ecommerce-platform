package com.nithingodugu.ecommerce.orderservice.service.impl;

import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationItem;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationRequest;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResponse;
import com.nithingodugu.ecommerce.common.contract.inventory.InventoryReservationResult;
import com.nithingodugu.ecommerce.common.contract.product.ProductPriceDetail;
import com.nithingodugu.ecommerce.common.contract.product.ProductPricingItem;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingRequest;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingResponse;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.common.event.OrderCreatedEvent;
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
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {

        log.info("Got product ids starting conection to client");

         /*
         Call product internal service to validate and calculate totalAmount
         * */

        ProductsPricingResponse pricing = productClient.quote(
                new ProductsPricingRequest(request.items().stream()
                        .map(item->
                                new ProductPricingItem(
                                        item.productId().toString(),
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

        /*
        * Create order and store orderItems
        * */

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderStatus(OrderStatus.VALIDATED);
        order.setTotalAmount(pricing.totalAmount());

        for(ProductPriceDetail product: pricing.items()){

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(Long.valueOf(product.productId()));
            item.setProductName(product.name());
            item.setPrice(product.unitPrice().doubleValue());
            item.setQuantity(product.quantity());

            order.getOrderItems().add(item);
        }

        orderRepository.save(order);

         /*
          Call inventory service to reserve stock
         */

        InventoryReservationResponse reservationResponse = inventoryClient.reservation(
                new InventoryReservationRequest(
                        order.getId().toString(),
                        request.items()
                                .stream()
                                .map(item->
                                        new InventoryReservationItem(
                                                item.productId().toString(),
                                                item.quantity()
                                        )
                                )
                                .toList()

                )
        );

        log.info(reservationResponse.message(), reservationResponse.status());

        if (reservationResponse.status() != InventoryReservationResult.SUCCESS){

            order.setOrderStatus(OrderStatus.REJECTED_OUT_OF_STOCK);
            orderRepository.save(order);

            throw new OutOfStockException(pricing.message());
        }

        order.setOrderStatus(OrderStatus.STOCK_RESERVED);
        orderRepository.save(order);


        return mapToResponse(order);

    }

    @Override
    public OrderResponse cancelOrder(UUID userId, Long id) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order id not found"));

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

        return mapToResponse(order);
    }

    @Override
    public OrderResponse getOrder(UUID userId, Long id) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order id not found"));

        return mapToResponse(order);
    }

    @Override
    public List<OrderResponse> getMyOrders(UUID userId) {
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
                order.getId(),
                order.getOrderStatus().name(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );
    }
}
