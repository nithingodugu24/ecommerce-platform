package com.nithingodugu.ecommerce.productservice.service;

import com.nithingodugu.ecommerce.productservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    //admin service
    ProductResponseDto createProduct(CreateProductRequestDto request);

    //user service
    ProductResponseDto getProductById(Long productId);

    Page<ProductResponseDto> getProducts(Pageable pageable);

    Page<ProductResponseDto> getProductsByName(String name, Pageable pageable);

    ProductResponseDto editProduct(Long id, EditProductRequestDto request);

    void deleteProduct(Long productId);

    //internal service
    List<ProductPricingResponse> getBulkPricing(BulkProductPricingRequest request);
}
