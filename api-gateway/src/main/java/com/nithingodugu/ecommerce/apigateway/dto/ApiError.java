package com.nithingodugu.ecommerce.apigateway.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
public record ApiError(
        int status,
        String message,
        Instant timestamp
) {}