package com.nithingodugu.ecommerce.productservice.controller.admin;

import com.nithingodugu.ecommerce.productservice.dto.CreateProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.EditProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class    AdminProductsController {

    private final ProductService productService;

    @PostMapping("")
    public ProductResponseDto createProduct(@Valid @RequestBody CreateProductRequestDto request){
        return productService.createProduct(request);
    }

    @PutMapping("/{productId}")
    public ProductResponseDto editProduct(@PathVariable String productId, EditProductRequestDto request){
        return productService.editProduct(productId, request);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId){
        productService.deleteProduct(productId);
        return ResponseEntity.accepted().build();
    }

}
