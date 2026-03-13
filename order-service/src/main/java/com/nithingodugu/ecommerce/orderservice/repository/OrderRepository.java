package com.nithingodugu.ecommerce.orderservice.repository;

import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);

    Optional<Order> findByOrderIdAndUserId(String orderId, String userId);

    Optional<Order> findByOrderId(String orderId);

    List<Order> findByOrderStatusAndCreatedAtBefore(OrderStatus orderStatus, Instant createdBefore);

}