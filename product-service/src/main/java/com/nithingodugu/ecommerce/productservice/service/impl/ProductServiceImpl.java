package com.nithingodugu.ecommerce.productservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.contract.product.*;
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
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
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

        log.info("Create product attempt",
                kv("name", request.name()),
                kv("category", request.category()),
                kv("price", request.price())
        );

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

        log.info("Product persisted",
                kv("productId", productId)
        );

        ProductCreatedEvent event = new ProductCreatedEvent(
                product.getProductId(),
                product.getName(),
                request.availableQuantity()
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        }catch (JsonProcessingException ex){

            log.error("Product event serialization failed",
                    kv("productId", productId),
                    kv("error", ex.getMessage()),
                    ex);

            throw new RuntimeException("Failed to serialize ProductCreated event", ex);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setAggregateId(product.getProductId());
        outbox.setTopic(KafkaTopics.PRODUCT_CREATED);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRequestId(MDC.get("requestId"));

        var currentSpan = Span.current().getSpanContext();
        if (currentSpan.isValid()) {
            outbox.setOriginalTraceId(currentSpan.getTraceId());
            outbox.setOriginalSpanId(currentSpan.getSpanId());
        }

        outboxEventRepository.save(outbox);

        log.info("Product created successfully",
                kv("productId", productId),
                kv("eventId", outbox.getEventId()),
                kv("topic", KafkaTopics.PRODUCT_CREATED)
        );

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

        log.info("Edit product attempt",
                kv("productId", productId));

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Edit product failed",
                            kv("reason", "PRODUCT_NOT_FOUND"),
                            kv("productId", productId));
                    return new ProductNotFoundException("Product not found");
                });

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(request.category());
        product.setActive(request.active());

        product = productRepository.save(product);

        log.info("Edit product success",
                kv("productId", productId));

        return mapToResponse(product);

    }

    @Override
    @Cacheable(cacheNames = PRODUCT_CACHE, key = "#productId")
    public ProductResponseDto getProductById(String productId) {

        log.debug("Get product attempt",
                kv("productId", productId));

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Get product failed",
                            kv("reason", "PRODUCT_NOT_FOUND"),
                            kv("productId", productId));
                    return new ProductNotFoundException("product not found");
                });

        log.debug("Get product success",
                kv("productId", productId));

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

        log.info("Delete product attempt",
                kv("productId", productId));

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Delete product failed",
                            kv("reason", "PRODUCT_NOT_FOUND"),
                            kv("productId", productId));
                    return new ProductNotFoundException("product not found");
                });

        productRepository.delete(product);

        log.info("Delete product success",
                kv("productId", productId));
    }

    @Override
    @Cacheable(cacheNames = PRODUCT_PAGE_CACHE, key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public PageResponse<ProductResponseDto> getProducts(Pageable pageable) {

        log.debug("Get products attempt",
                kv("page", pageable.getPageNumber()),
                kv("size", pageable.getPageSize()));

        Page<Product> products = productRepository.findAll(pageable);

        Page<ProductResponseDto> dtoPage = products.map(this::mapToResponse);

        log.debug("Get products success",
                kv("page", pageable.getPageNumber()),
                kv("size", pageable.getPageSize()),
                kv("resultCount", products.getNumberOfElements()));

        return new PageResponse<>(dtoPage);

    }

    @Override
    @Cacheable(cacheNames = PRODUCT_SEARCH_CACHE, key = "#name + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public PageResponse<ProductResponseDto> getProductsByName(String name, Pageable pageable) {

        log.debug("Search products attempt",
                kv("name", name),
                kv("page", pageable.getPageNumber()),
                kv("size", pageable.getPageSize()));

        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);

        Page<ProductResponseDto> dtoPage = products.map(this::mapToResponse);

        log.debug("Search products success",
                kv("name", name),
                kv("resultCount", products.getNumberOfElements()));

       return new PageResponse<>(dtoPage);
    }

    @Override
    public ProductsPricingResponse quote(ProductsPricingRequest request) {

        log.info("Pricing quote attempt",
                kv("itemsCount", request.items().size()));

        Map<String, Integer> requestedQtyMap = request.items().stream()
                .collect(Collectors.toMap(
                        ProductPricingItem::productId,
                        ProductPricingItem::quantity,
                        Integer::sum
                ));


        List<Product> products = productRepository.findByProductIdIn(requestedQtyMap.keySet());

        if (products.size() != requestedQtyMap.size()) {

            log.warn("Pricing quote failed",
                    kv("reason", "PRODUCTS_NOT_FOUND"),
                    kv("requestedCount", requestedQtyMap.size()),
                    kv("foundCount", products.size()));

            return new ProductsPricingResponse(
                    ProductsPricingStatus.INVALID_PRODUCT,
                    "One or more products not found",
                    null,
                    null
            );
        }

        List<ProductPriceDetail> priceDetails = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (Product product : products) {
            if (!product.getActive()) {

                log.warn("Pricing quote failed",
                        kv("reason", "PRODUCTS_INACTIVE")
                );

                return new ProductsPricingResponse(
                        ProductsPricingStatus.INVALID_PRODUCT,
                        "Product inactive: " + product.getProductId(),
                        null,
                        null
                );
            }

            Integer qty = requestedQtyMap.get(product.getProductId());

            BigDecimal lineTotal = BigDecimal.valueOf(product.getPrice())
                    .multiply(BigDecimal.valueOf(qty));

            total = total.add(lineTotal);

            priceDetails.add(
                    new ProductPriceDetail(
                            product.getProductId(),
                            product.getName(),
                            qty,
                            BigDecimal.valueOf(product.getPrice()),
                            lineTotal
                    )
            );
        }

        log.info("Pricing quote success");

        return new ProductsPricingResponse(
                ProductsPricingStatus.VERIFIED,
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
