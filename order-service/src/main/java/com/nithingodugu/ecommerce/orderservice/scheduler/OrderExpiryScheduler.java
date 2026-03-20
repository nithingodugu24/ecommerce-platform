package com.nithingodugu.ecommerce.orderservice.scheduler;

import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;
import com.nithingodugu.ecommerce.orderservice.repository.OrderRepository;
import com.nithingodugu.ecommerce.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000)
    public void cancelUnpaidOrders(){

        Instant cutOff = Instant.now().minus(15, ChronoUnit.MINUTES);

        List<Order> staleOrders = orderRepository
                .findByOrderStatusAndCreatedAtBefore(OrderStatus.CREATED, cutOff);

        for (Order order: staleOrders){
            orderService.cancelOrder(order.getUserId(), order.getOrderId());
        }

    }



}
