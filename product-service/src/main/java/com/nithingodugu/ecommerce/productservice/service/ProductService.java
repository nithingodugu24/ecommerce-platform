package com.nithingodugu.ecommerce.productservice.service;

import com.nithingodugu.ecommerce.productservice.dto.CreateProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.EditProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponseDto createProduct(CreateProductRequestDto request);

    ProductResponseDto getProductById(Long productId);

    Page<ProductResponseDto> getProducts(Pageable pageable);

    Page<ProductResponseDto> getProductsByName(String name, Pageable pageable);

    ProductResponseDto editProduct(Long id, EditProductRequestDto request);

    void deleteProduct(Long productId);
}
