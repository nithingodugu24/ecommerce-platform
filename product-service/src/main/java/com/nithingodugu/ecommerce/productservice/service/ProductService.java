package com.nithingodugu.ecommerce.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingRequest;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingResponse;
import com.nithingodugu.ecommerce.productservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    //admin service
    ProductResponseDto createProduct(CreateProductRequestDto request);

    //user service
    ProductResponseDto getProductById(String productId);

    PageResponse<ProductResponseDto> getProducts(Pageable pageable);

    PageResponse<ProductResponseDto> getProductsByName(String name, Pageable pageable);

    ProductResponseDto editProduct(String productId, EditProductRequestDto request);

    void deleteProduct(String productId);

    //internal service
    ProductsPricingResponse quote(ProductsPricingRequest request);
}
