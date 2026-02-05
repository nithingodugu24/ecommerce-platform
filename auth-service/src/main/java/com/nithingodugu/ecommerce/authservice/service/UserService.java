package com.nithingodugu.ecommerce.authservice.service;

import com.nithingodugu.ecommerce.authservice.dto.*;
import jakarta.validation.Valid;

import java.security.DigestException;

public interface UserService {
    RegisterResponse register(RegisterRequest request);

    LoginResponse login(@Valid LoginRequest request);

    LoginResponse refresh(@Valid RefreshTokenRequest request);

    void logout(String refreshToken);
}
