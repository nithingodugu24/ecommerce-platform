package com.nithingodugu.ecommerce.paymentservice.client;

import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/internal/orders/{userid}/{orderId}")
    public OrderDetailsResponse getOrderDetails(
            @PathVariable("userid") String userId,
            @PathVariable("orderId") String orderId
    );
}
