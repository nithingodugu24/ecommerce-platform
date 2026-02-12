package com.nithingodugu.ecommerce.productservice.service.impl;

import com.nithingodugu.ecommerce.productservice.domain.entity.Product;
import com.nithingodugu.ecommerce.productservice.dto.CreateProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.EditProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import com.nithingodugu.ecommerce.productservice.exceptions.ProductNotFoundException;
import com.nithingodugu.ecommerce.productservice.repository.ProductRepository;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductResponseDto createProduct(CreateProductRequestDto request) {

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .active(true)
                .build();

        product = productRepository.save(product);

        return mapToResponse(product);
    }

    @Override
    public Page<ProductResponseDto> getProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);

        return products.map(this::mapToResponse);
    }

    @Override
    public Page<ProductResponseDto> getProductsByName(String name, Pageable pageable) {

       Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);

       return products.map(this::mapToResponse);
    }

    @Override
    public ProductResponseDto editProduct(Long id, EditProductRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("product not found"));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(request.category());
        product.setActive(request.active());

        product = productRepository.save(product);

        return mapToResponse(product);

    }

    @Override
    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("product not found"));

        return mapToResponse(product);
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("product not found"));

        //logic any before delete

        productRepository.delete(product);
    }



    private ProductResponseDto mapToResponse(Product product) {
        return new ProductResponseDto(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getActive(),
                product.getCreatedAt()
        );
    }

}
