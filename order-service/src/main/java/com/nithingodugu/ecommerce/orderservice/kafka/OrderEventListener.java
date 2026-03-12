package com.nithingodugu.ecommerce.orderservice.kafka;

import com.nithingodugu.ecommerce.common.event.InventoryFailedEvent;
import com.nithingodugu.ecommerce.common.event.PaymentAuthorizedEvent;
import com.nithingodugu.ecommerce.common.event.PaymentFailedEvent;
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
        Order order = orderRepository.findByOrderId(event.getOrderId()).orElseThrow();
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @KafkaListener(topics = "payment.authorized")
    public void handlePaymentAuthorized(PaymentAuthorizedEvent event){
        Order order = orderRepository.findByOrderId(event.orderId()).orElse(null);
        if (order == null){
            log.error("Invalid orderid from event");
            return;
        }

        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    @KafkaListener(topics = "payment.failed")
    public void handlePaymentFailed(PaymentFailedEvent event){
        Order order = orderRepository.findByOrderId(event.orderId()).orElse(null);
        if (order == null){
            log.error("Invalid orderid from event2");
            return;
        }

        order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
        //trigger release stock
        orderRepository.save(order);
    }

}
