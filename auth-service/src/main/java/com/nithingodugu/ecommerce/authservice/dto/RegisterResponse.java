package com.nithingodugu.ecommerce.authservice.dto;

import com.nithingodugu.ecommerce.authservice.domain.enums.UserRole;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {
}
