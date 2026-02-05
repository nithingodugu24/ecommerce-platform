package com.nithingodugu.ecommerce.authservice.controller;

import com.nithingodugu.ecommerce.authservice.dto.LoginRequest;
import com.nithingodugu.ecommerce.authservice.dto.LoginResponse;
import com.nithingodugu.ecommerce.authservice.dto.RegisterRequest;
import com.nithingodugu.ecommerce.authservice.dto.RegisterResponse;
import com.nithingodugu.ecommerce.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request){
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return userService.login(request);
    }
}
