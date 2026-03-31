package com.nithingodugu.ecommerce.cartservice.client;

import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingRequest;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {

    @PostMapping("/internal/products/quote")
    ProductsPricingResponse quote(@RequestBody ProductsPricingRequest request);
}
