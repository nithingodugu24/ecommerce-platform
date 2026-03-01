package com.nithingodugu.ecommerce.authservice.dto;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
