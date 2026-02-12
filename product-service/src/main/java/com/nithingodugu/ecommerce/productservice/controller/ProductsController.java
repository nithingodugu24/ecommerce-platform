package com.nithingodugu.ecommerce.productservice.controller;

import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductsController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long productId){
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("")
    public ResponseEntity<Page<ProductResponseDto>> getProducts(Pageable pageable){

        Page<ProductResponseDto> products = productService.getProducts(pageable);

        return ResponseEntity.ok(products);
    }

    @GetMapping("/search/{name}")
    public ResponseEntity<Page<ProductResponseDto>> getProductsByName(@PathVariable String name, Pageable pageable){

        Page<ProductResponseDto> products = productService.getProductsByName(name, pageable);

        return ResponseEntity.ok(products);
    }
}
