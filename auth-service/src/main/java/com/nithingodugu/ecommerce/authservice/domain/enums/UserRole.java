package com.nithingodugu.ecommerce.authservice.domain.enums;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

public enum UserRole implements GrantedAuthority {
    USER,
    ADMIN;

    @Override
    public @Nullable String getAuthority() {
        return name();
    }
}
