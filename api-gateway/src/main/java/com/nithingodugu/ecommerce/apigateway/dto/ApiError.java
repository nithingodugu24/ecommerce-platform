package com.nithingodugu.ecommerce.apigateway.dto;

import java.time.Instant;

public record ApiError(
        int status,
        String message,
        Instant timestamp
) {}