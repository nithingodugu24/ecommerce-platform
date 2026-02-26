package com.nithingodugu.ecommerce.orderservice.kafka;

import com.nithingodugu.ecommerce.common.event.InventoryFailedEvent;
import com.nithingodugu.ecommerce.orderservice.domain.entity.Order;
import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;
import com.nithingodugu.ecommerce.orderservice.dto.OrderResponse;
import com.nithingodugu.ecommerce.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "inventory.failed")
    @Transactional
    public void handleInventoryFailed(InventoryFailedEvent event){
        log.info("event recieved of inventory failed");
        Order order = orderRepository.getById(event.getOrderId());
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

}
