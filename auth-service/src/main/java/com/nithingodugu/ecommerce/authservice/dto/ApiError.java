package com.nithingodugu.ecommerce.authservice.dto;

import java.time.Instant;

public record ApiError(
        int status,
        String message,
        Instant timestamp
) {}