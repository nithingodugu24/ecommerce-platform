package com.nithingodugu.ecommerce.authservice.service;

import com.nithingodugu.ecommerce.authservice.dto.*;
import jakarta.validation.Valid;

import java.security.DigestException;

public interface UserService {
    RegisterResponse register(RegisterRequest request);

    AuthTokens login(@Valid LoginRequest request);

    AuthTokens refresh(@Valid String refreshToken);

    void logout(String refreshToken);
}
