package com.nithingodugu.ecommerce.authservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Builder
public record ApiError(
        int status,
        String message,
        Instant timestamp
) {}