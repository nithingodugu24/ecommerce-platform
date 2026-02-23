package com.nithingodugu.ecommerce.orderservice.repository;

import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

}