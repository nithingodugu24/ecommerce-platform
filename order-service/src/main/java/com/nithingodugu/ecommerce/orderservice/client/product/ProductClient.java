package com.nithingodugu.ecommerce.orderservice.client.product;

import com.nithingodugu.ecommerce.orderservice.dto.BulkProductPricingRequest;
import com.nithingodugu.ecommerce.orderservice.dto.ProductPricingResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@AllArgsConstructor
public class ProductClient {

    private final WebClient productWebConfig;

    public List<ProductPricingResponse> getBulkProductPricing(List<Long> ids){

        BulkProductPricingRequest request = new BulkProductPricingRequest(ids);

        return productWebConfig.post()
                .uri("internal/products/pricing/bulk")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ProductPricingResponse.class)
                .collectList()
                .block();
    }

}
