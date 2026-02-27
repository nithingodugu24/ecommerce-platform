package com.nithingodugu.ecommerce.authservice.dto;

import com.nithingodugu.ecommerce.authservice.domain.enums.UserRole;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserStatus;

import java.util.UUID;

public record AuthTokens(
        UUID userId,
        String email,
        UserRole role,
        UserStatus status,
        String accessToken,
        Long accessTokenExpiresIn,
        String refreshToken
) {}