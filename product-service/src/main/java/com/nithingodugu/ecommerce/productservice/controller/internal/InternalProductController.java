package com.nithingodugu.ecommerce.productservice.controller.internal;

import com.nithingodugu.ecommerce.productservice.dto.BulkProductPricingRequest;
import com.nithingodugu.ecommerce.productservice.dto.ProductPricingRequest;
import com.nithingodugu.ecommerce.productservice.dto.ProductPricingResponse;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal/products/")
@AllArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @PostMapping("/pricing/bulk")
    public List<ProductPricingResponse> getBulkPricing(
            @RequestBody BulkProductPricingRequest request
    ){
        log.info("new internal request");
        return productService.getBulkPricing(request);
    }
}
