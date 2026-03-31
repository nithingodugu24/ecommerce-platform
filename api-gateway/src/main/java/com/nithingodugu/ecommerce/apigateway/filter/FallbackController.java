package com.nithingodugu.ecommerce.apigateway.filter;

import com.nithingodugu.ecommerce.apigateway.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/payment")
    public ResponseEntity<ApiError> paymentFallback() {
        ApiError error = ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Payment Service temporarily unavailable")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(error.status()).body(error);
    }

    @RequestMapping("/fallback/order")
    public ResponseEntity<ApiError> orderFallback() {
        ApiError error = ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Order Service temporarily unavailable")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(error.status()).body(error);
    }

    @RequestMapping("/fallback/product")
    public ResponseEntity<ApiError> productFallback() {
        ApiError error = ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Product Service temporarily unavailable")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(error.status()).body(error);
    }

    @RequestMapping("/fallback/inventory")
    public ResponseEntity<ApiError> inventoryFallback() {
        ApiError error = ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Inventory Service temporarily unavailable")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(error.status()).body(error);
    }

    @RequestMapping("/fallback/auth")
    public ResponseEntity<ApiError> authFallback() {
        ApiError error = ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Authentication Service temporarily unavailable")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(error.status()).body(error);
    }

    @RequestMapping("/fallback/cart")
    public ResponseEntity<ApiError> cartFallback() {
        ApiError error = ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Cart Service temporarily unavailable")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(error.status()).body(error);
    }
}