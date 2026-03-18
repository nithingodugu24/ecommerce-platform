package com.nithingodugu.ecommerce.productservice.controller.internal;

import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingRequest;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingResponse;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
@AllArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @PostMapping("/quote")
    public ProductsPricingResponse quote(
            @RequestBody ProductsPricingRequest request
    ){
        return productService.quote(request);
    }
}
