package com.nithingodugu.ecommerce.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "payment-service")
public interface PaymentClient {

//    @PostMapping("/internal/payments/authorize")
//    PaymentResponse authorize(@RequestBody AuthorizePaymentRequest request);
}
