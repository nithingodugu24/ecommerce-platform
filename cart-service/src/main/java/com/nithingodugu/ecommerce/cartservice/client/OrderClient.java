package com.nithingodugu.ecommerce.cartservice.client;

import com.nithingodugu.ecommerce.cartservice.contract.CreateOrderRequest;
import com.nithingodugu.ecommerce.cartservice.contract.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PostMapping("/orders")
    OrderResponse createOrder(
            @RequestBody CreateOrderRequest request
    );

}
