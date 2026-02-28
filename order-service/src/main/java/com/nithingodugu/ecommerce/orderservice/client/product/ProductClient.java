package com.nithingodugu.ecommerce.orderservice.client.product;

import com.nithingodugu.ecommerce.orderservice.dto.BulkProductPricingRequest;
import com.nithingodugu.ecommerce.orderservice.dto.ProductPricingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductClient {

    @PostMapping("/internal/products/pricing/bulk")
    List<ProductPricingResponse> getBulkProductPricing(
            @RequestBody BulkProductPricingRequest request
    );
}
