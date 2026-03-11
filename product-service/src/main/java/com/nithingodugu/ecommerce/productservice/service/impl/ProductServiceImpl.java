package com.nithingodugu.ecommerce.productservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.contract.product.ProductPriceDetail;
import com.nithingodugu.ecommerce.common.contract.product.ProductPricingItem;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingRequest;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingResponse;
import com.nithingodugu.ecommerce.productservice.domain.entity.Product;
import com.nithingodugu.ecommerce.productservice.dto.*;
import com.nithingodugu.ecommerce.productservice.exceptions.ProductNotFoundException;
import com.nithingodugu.ecommerce.productservice.outbox.KafkaTopics;
import com.nithingodugu.ecommerce.productservice.outbox.entity.OutboxEvent;
import com.nithingodugu.ecommerce.productservice.outbox.entity.OutboxStatus;
import com.nithingodugu.ecommerce.productservice.outbox.repository.OutboxEventRepository;
import com.nithingodugu.ecommerce.productservice.repository.ProductRepository;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import com.nithingodugu.ecommerce.productservice.util.IdGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    private static final String PRODUCT_CACHE = "product";
    private static final String PRODUCT_PAGE_CACHE = "product_page";
    private static final String PRODUCT_SEARCH_CACHE = "product_search";

    @Override
    @Caching(
            put = @CachePut(cacheNames = PRODUCT_CACHE, key = "#result.productId"),
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_PAGE_CACHE, allEntries = true),
                    @CacheEvict(cacheNames = PRODUCT_SEARCH_CACHE, allEntries = true)
            }
    )
    @Transactional
    public ProductResponseDto createProduct(CreateProductRequestDto request) {

        String productId = idGenerator.generateProductId();

        Product product = Product.builder()
                .productId(productId)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .active(true)
                .build();

        productRepository.save(product);

        ProductCreatedEvent event = new ProductCreatedEvent(
                product.getProductId(),
                product.getName(),
                request.availableQuantity()
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        }catch (JsonProcessingException ex){
            throw new RuntimeException("Failed to serialize ProductCreated event", ex);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setAggregateId(product.getProductId());
        outbox.setTopic(KafkaTopics.PRODUCT_CREATED);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);

        outboxEventRepository.save(outbox);

        return mapToResponse(product);
    }

    @Override
    @Caching(
            put = @CachePut(cacheNames = PRODUCT_CACHE, key = "#productId"),
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_PAGE_CACHE, allEntries = true),
                    @CacheEvict(cacheNames = PRODUCT_SEARCH_CACHE, allEntries = true)
            }
    )
    public ProductResponseDto editProduct(String productId, EditProductRequestDto request) {

        Product product = productRepository.findByProductId(productId)
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
    @Cacheable(cacheNames = PRODUCT_CACHE, key = "#productId")
    public ProductResponseDto getProductById(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("product not found"));

        return mapToResponse(product);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_CACHE, key = "#productId"),
                    @CacheEvict(cacheNames = PRODUCT_PAGE_CACHE, allEntries = true),
                    @CacheEvict(cacheNames = PRODUCT_SEARCH_CACHE, allEntries = true)
            }
    )


    public void deleteProduct(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("product not found"));


        productRepository.delete(product);
    }

    @Override
    @Cacheable(cacheNames = PRODUCT_PAGE_CACHE, key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public PageResponse<ProductResponseDto> getProducts(Pageable pageable) {

        Page<Product> products = productRepository.findAll(pageable);
        Page<ProductResponseDto> dtoPage = products.map(this::mapToResponse);

        return new PageResponse<>(dtoPage);

    }

    @Override
    @Cacheable(cacheNames = PRODUCT_SEARCH_CACHE, key = "#name + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public PageResponse<ProductResponseDto> getProductsByName(String name, Pageable pageable) {

       Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);
        Page<ProductResponseDto> dtoPage = products.map(this::mapToResponse);

       return new PageResponse<>(dtoPage);
    }

    @Override
    public ProductsPricingResponse quote(ProductsPricingRequest request) {

        Map<String, Integer> requestedQtyMap = request.items().stream()
                .collect(Collectors.toMap(
                        ProductPricingItem::productId,
                        ProductPricingItem::quantity,
                        Integer::sum
                ));


        List<Product> products = productRepository.findByProductIdIn(requestedQtyMap.keySet());

        if (products.size() != requestedQtyMap.size()) {
            return new ProductsPricingResponse(
                    false,
                    "One or more products not found",
                    null,
                    null
            );
        }

        List<ProductPriceDetail> priceDetails = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (Product product : products) {

            if (!product.getActive()) {
                return new ProductsPricingResponse(
                        false,
                        "Product inactive: " + product.getId(),
                        null,
                        null
                );
            }

            Integer qty = requestedQtyMap.get(product.getId().toString());

            BigDecimal lineTotal = BigDecimal.valueOf(product.getPrice())
                    .multiply(BigDecimal.valueOf(qty));

            total = total.add(lineTotal);

            priceDetails.add(
                    new ProductPriceDetail(
                            product.getId().toString(),
                            product.getName(),
                            qty,
                            BigDecimal.valueOf(product.getPrice()),
                            lineTotal
                    )
            );
        }
        return new ProductsPricingResponse(
                true,
                "success",
                priceDetails,
                total
        );

    }

    private ProductResponseDto mapToResponse(Product product) {
        return new ProductResponseDto(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getActive(),
                product.getCreatedAt()
        );
    }


}
