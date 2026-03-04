package com.nithingodugu.ecommerce.productservice.service.impl;

import com.nithingodugu.ecommerce.common.contract.product.ProductPriceDetail;
import com.nithingodugu.ecommerce.common.contract.product.ProductPricingItem;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingRequest;
import com.nithingodugu.ecommerce.common.contract.product.ProductsPricingResponse;
import com.nithingodugu.ecommerce.productservice.domain.entity.Product;
import com.nithingodugu.ecommerce.productservice.dto.*;
import com.nithingodugu.ecommerce.productservice.exceptions.ProductNotFoundException;
import com.nithingodugu.ecommerce.productservice.repository.ProductRepository;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.nithingodugu.ecommerce.common.event.ProductCreatedEvent;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PRODUCT_CACHE = "product";
    private static final String PRODUCT_PAGE_CACHE = "product_page";
    private static final String PRODUCT_SEARCH_CACHE = "product_search";

    @Override
    @Caching(
            put = @CachePut(cacheNames = PRODUCT_CACHE, key = "#result.id"),
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_PAGE_CACHE, allEntries = true),
                    @CacheEvict(cacheNames = PRODUCT_SEARCH_CACHE, allEntries = true)
            }
    )
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

    @Caching(
            put = @CachePut(cacheNames = PRODUCT_CACHE, key = "#productId"),
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_PAGE_CACHE, allEntries = true),
                    @CacheEvict(cacheNames = PRODUCT_SEARCH_CACHE, allEntries = true)
            }
    )
    public ProductResponseDto editProduct(Long productId, EditProductRequestDto request) {

        Product product = productRepository.findById(productId)
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
    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
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
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("product not found"));

        //logic any before delete

        productRepository.delete(product);
    }

    @Override
    @Cacheable(cacheNames = PRODUCT_PAGE_CACHE, key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<ProductResponseDto> getProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);

        return products.map(this::mapToResponse);
    }

    @Override
    @Cacheable(cacheNames = PRODUCT_SEARCH_CACHE, key = "#name + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<ProductResponseDto> getProductsByName(String name, Pageable pageable) {

       Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);

       return products.map(this::mapToResponse);
    }

    @Override
    public ProductsPricingResponse quote(ProductsPricingRequest request) {

        Map<String, Integer> requestedQtyMap = request.items().stream()
                .collect(Collectors.toMap(
                        ProductPricingItem::productId,
                        ProductPricingItem::quantity,
                        Integer::sum
                ));


        List<Product> products = productRepository.findByIdIn(requestedQtyMap.keySet());

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
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getActive(),
                product.getCreatedAt()
        );
    }
}
