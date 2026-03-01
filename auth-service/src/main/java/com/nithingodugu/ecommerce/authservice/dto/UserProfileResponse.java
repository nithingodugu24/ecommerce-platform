package com.nithingodugu.ecommerce.authservice.dto;

import com.nithingodugu.ecommerce.authservice.domain.enums.UserRole;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserStatus;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {
}
