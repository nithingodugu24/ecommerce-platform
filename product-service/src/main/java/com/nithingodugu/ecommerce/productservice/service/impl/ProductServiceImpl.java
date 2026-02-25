package com.nithingodugu.ecommerce.productservice.service.impl;

import com.nithingodugu.ecommerce.productservice.domain.entity.Product;
import com.nithingodugu.ecommerce.productservice.dto.*;
import com.nithingodugu.ecommerce.productservice.exceptions.ProductNotFoundException;
import com.nithingodugu.ecommerce.productservice.repository.ProductRepository;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import com.nithingodugu.ecommerce.productservice.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
//import com.nithingodugu.common.event.ProductCreatedEvent;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Autowired
    private KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

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

        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setProductId(product.getId());
        event.setName(product.getName());
        event.setInitialQuantity(request.availableQuantity());

        kafkaTemplate.send("product.created",event);

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

    @Override
    public List<ProductPricingResponse> getBulkPricing(BulkProductPricingRequest request) {

        List<Long> ids = request.ids;

        if(ids == null || ids.isEmpty()){
            return List.of();
        }

        return productRepository.findPricingByIds(ids);
    }
}
