package com.nithingodugu.ecommerce.authservice.service;


import com.nithingodugu.ecommerce.authservice.dto.AuthTokens;
import com.nithingodugu.ecommerce.authservice.dto.LoginRequest;
import com.nithingodugu.ecommerce.authservice.dto.RegisterRequest;
import com.nithingodugu.ecommerce.authservice.dto.RegisterResponse;
import jakarta.validation.Valid;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);

    AuthTokens login(@Valid LoginRequest request);

    AuthTokens refresh(@Valid String refreshToken);

    void logout(String refreshToken);
}
