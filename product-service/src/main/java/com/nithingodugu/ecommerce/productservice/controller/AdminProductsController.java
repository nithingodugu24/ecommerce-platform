package com.nithingodugu.ecommerce.productservice.controller;

import com.nithingodugu.ecommerce.productservice.dto.CreateProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.EditProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductsController {

    private final ProductService productService;

    @PostMapping("")
    public ProductResponseDto createProduct(CreateProductRequestDto request){
        return productService.createProduct(request);
    }

    @PutMapping("/{productId}")
    public ProductResponseDto editProduct(@PathVariable Long productId, EditProductRequestDto request){
        return productService.editProduct(productId, request);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId){
        productService.deleteProduct(productId);
        return ResponseEntity.accepted().build();
    }

}
