package com.nithingodugu.ecommerce.productservice.controller.admin;

import com.nithingodugu.ecommerce.productservice.dto.CreateProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.EditProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductsController {

    private final ProductService productService;

    @GetMapping("")
    public ResponseEntity<Page<ProductResponseDto>> getProducts(Pageable pageable){

        Page<ProductResponseDto> products = productService.getProducts(pageable);

        return ResponseEntity.ok(products);
    }

    @PostMapping("")
    public ProductResponseDto createProduct(@Valid @RequestBody CreateProductRequestDto request){
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
