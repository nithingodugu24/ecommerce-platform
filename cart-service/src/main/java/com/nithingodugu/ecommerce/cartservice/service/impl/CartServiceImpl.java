package com.nithingodugu.ecommerce.cartservice.service.impl;

import com.nithingodugu.ecommerce.cartservice.client.OrderClient;
import com.nithingodugu.ecommerce.cartservice.client.ProductClient;
import com.nithingodugu.ecommerce.cartservice.contract.CreateOrderRequest;
import com.nithingodugu.ecommerce.cartservice.contract.OrderItemRequest;
import com.nithingodugu.ecommerce.cartservice.contract.OrderResponse;
import com.nithingodugu.ecommerce.cartservice.domain.entity.Cart;
import com.nithingodugu.ecommerce.cartservice.domain.entity.CartItem;
import com.nithingodugu.ecommerce.cartservice.dto.AddToCartRequest;
import com.nithingodugu.ecommerce.cartservice.dto.CartItemRequest;
import com.nithingodugu.ecommerce.cartservice.dto.CartItemResponse;
import com.nithingodugu.ecommerce.cartservice.dto.CartResponse;
import com.nithingodugu.ecommerce.cartservice.exceptions.CartNotFoundException;
import com.nithingodugu.ecommerce.cartservice.exceptions.ItemNotFoundException;
import com.nithingodugu.ecommerce.cartservice.exceptions.ServiceUnavailableException;
import com.nithingodugu.ecommerce.cartservice.repository.CartRepository;
import com.nithingodugu.ecommerce.cartservice.service.CartService;
import com.nithingodugu.ecommerce.common.contract.product.*;
import com.nithingodugu.ecommerce.common.exceptions.InvalidProductException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RequiredArgsConstructor
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final OrderClient orderClient;

    @Override
    public CartResponse getCart(String userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);

        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        log.info("AddToCart request",
                kv("userId", userId),
                kv("itemsCount", request.items().size())
        );

        long startPricingRequest = System.currentTimeMillis();

        ProductsPricingResponse pricing = getPricing(
                new ProductsPricingRequest(request.items().stream()
                        .map(item->
                                new ProductPricingItem(
                                        item.productId(),
                                        item.quantity()
                                )
                        )
                        .toList()
                )
        );

        log.info("Pricing response received",
                kv("success", pricing.status().name()),
                kv("message", pricing.message()),
                kv("durationMs", System.currentTimeMillis() - startPricingRequest)
        );

        guardPricingStatus(pricing);

        Map<String, BigDecimal> priceMap = buildPriceMap(pricing);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUserId(userId);
                    c.setItems(new ArrayList<>());
                    return c;
                });

        Map<String, CartItem> existing = cart.getItems().stream()
                .collect(Collectors.toMap(CartItem::getProductId, i -> i));

        for (CartItemRequest incoming : request.items()) {
            BigDecimal unitPrice = priceMap.get(incoming.productId());
            if (existing.containsKey(incoming.productId())) {
                CartItem item = existing.get(incoming.productId());
                item.setQuantity(item.getQuantity() + incoming.quantity());
                item.setPriceSnapshot(unitPrice);
            } else {
                CartItem item = new CartItem();
                item.setProductId(incoming.productId());
                item.setQuantity(incoming.quantity());
                item.setPriceSnapshot(unitPrice);
                cart.addItem(item);
            }
        }

        Cart saved = cartRepository.save(cart);
        log.info("Cart saved after addToCart",
                kv("userId", userId),
                kv("totalItems", saved.getItems().size()));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(String userId, String productId, int quantity) {

        log.info("UpdateItemQuantity request",
                kv("userId", userId),
                kv("productId", productId),
                kv("quantity", quantity));

        Cart cart = requireCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException(
                        "Product %s not found in cart".formatted(productId)));

        if (quantity <= 0) {
            // remove the item from cart
            cart.getItems().remove(item);
            log.info("Item removed via zero quantity",
                    kv("userId", userId), kv("productId", productId));
        } else {
            // re-fetch the price and update
            ProductsPricingResponse pricing = getPricing(
                    new ProductsPricingRequest(List.of(new ProductPricingItem(productId, quantity)))
            );
            guardPricingStatus(pricing);

            item.setQuantity(quantity);
            item.setPriceSnapshot(buildPriceMap(pricing).getOrDefault(productId, item.getPriceSnapshot()));
        }

        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(String userId, String productId) {

        log.info("RemoveItem request",
                kv("userId", userId),
                kv("productId", productId));

        Cart cart = requireCart(userId);

        boolean removed = cart.getItems()
                .removeIf(i -> i.getProductId().equals(productId));

        if (!removed) {
            throw new ItemNotFoundException(
                    "Product %s not found in cart".formatted(productId));
        }

        log.info("Item removed from cart",
                kv("userId", userId), kv("productId", productId));

        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        log.info("ClearCart request", kv("userId", userId));

        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
            log.info("Cart cleared", kv("userId", userId));
        });
    }

    @Override
    @Transactional
    public OrderResponse checkout(String userId) {
        log.info("Checkout request", kv("userId", userId));

        Cart cart = requireCart(userId);

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                cart.getItems().stream()
                        .map(i -> new OrderItemRequest(
                                i.getProductId(),
                                i.getQuantity()
                        ))
                        .toList()
        );

        OrderResponse orderResponse = createOrderClient(orderRequest);

        log.info("Order created, clearing cart",
                kv("userId", userId),
                kv("orderId", orderResponse.orderId()));

        // Clear the cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return orderResponse;
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "orderFallback")
    public OrderResponse createOrderClient(CreateOrderRequest request) {
        return orderClient.createOrder(request);
    }

    public OrderResponse orderFallback(CreateOrderRequest request, Exception ex) {
        log.error("Order service failed", kv("error", ex));
        throw new RuntimeException("Order service temporarily unavailable. Please try again.");
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "productFallback")
    public ProductsPricingResponse getPricing(ProductsPricingRequest request) {
        return productClient.quote(request);
    }

    public ProductsPricingResponse productFallback(ProductsPricingRequest request, Exception ex) {
        log.error("Product service failed",
                kv("error", ex));

        return new ProductsPricingResponse(
                ProductsPricingStatus.FAILED,
                "Product service unavailable",
                List.of(),
                null
        );
    }

    private Map<String, BigDecimal> buildPriceMap(ProductsPricingResponse pricing) {
        return pricing.items().stream()
                .collect(Collectors.toMap(
                        ProductPriceDetail::productId,
                        ProductPriceDetail::unitPrice
                ));
    }

    private Cart requireCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(
                        "No cart found for user %s".formatted(userId)));
    }



    private void guardPricingStatus(ProductsPricingResponse pricing) {
        if (pricing.status() == ProductsPricingStatus.INVALID_PRODUCT) {
            log.warn("Pricing guard: invalid product", kv("message", pricing.message()));
            throw new InvalidProductException(pricing.message());
        }
        if (pricing.status() == ProductsPricingStatus.FAILED) {
            throw new ServiceUnavailableException(pricing.message());
        }
    }

    private CartResponse mapToResponse(Cart cart){

        if (cart == null){
            return new CartResponse(
                    new ArrayList<>()
            );
        }

        return new CartResponse(
            cart.getItems()
                    .stream()
                    .map(item -> new CartItemResponse(
                            item.getProductId(),
                            item.getQuantity(),
                            item.getPriceSnapshot()
                    ))
                    .toList()
        );
    }
}
