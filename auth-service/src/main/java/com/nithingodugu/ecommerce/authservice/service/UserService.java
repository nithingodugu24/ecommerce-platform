package com.nithingodugu.ecommerce.authservice.service;

import com.nithingodugu.ecommerce.authservice.dto.LoginRequest;
import com.nithingodugu.ecommerce.authservice.dto.LoginResponse;
import com.nithingodugu.ecommerce.authservice.dto.RegisterRequest;
import com.nithingodugu.ecommerce.authservice.dto.RegisterResponse;
import jakarta.validation.Valid;

import java.security.DigestException;

public interface UserService {
    RegisterResponse register(RegisterRequest request);

    LoginResponse login(@Valid LoginRequest request);
}
