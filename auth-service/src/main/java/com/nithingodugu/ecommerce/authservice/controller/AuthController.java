package com.nithingodugu.ecommerce.authservice.controller;

import com.nithingodugu.ecommerce.authservice.dto.*;
import com.nithingodugu.ecommerce.authservice.security.jwt.JwtUtil;
import com.nithingodugu.ecommerce.authservice.service.AuthService;
import com.nithingodugu.ecommerce.authservice.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ){
        AuthTokens authTokens = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from(
                "refresh_token", authTokens.refreshToken()
        )
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(jwtUtil.getRefreshExpiration())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return new LoginResponse(
                authTokens.userId(),
                authTokens.email(),
                authTokens.role(),
                authTokens.status(),
                authTokens.accessToken(),
                authTokens.accessTokenExpiresIn()
        );
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse refresh(
            @CookieValue(name = "refresh_token") String refreshToken,
            HttpServletResponse response
    ){

        AuthTokens authTokens = authService.refresh(refreshToken);

        ResponseCookie refreshCookie = ResponseCookie.from(
                        "refresh_token", authTokens.refreshToken()
                )
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(jwtUtil.getRefreshExpiration())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return new LoginResponse(
                authTokens.userId(),
                authTokens.email(),
                authTokens.role(),
                authTokens.status(),
                authTokens.accessToken(),
                authTokens.accessTokenExpiresIn()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = "refresh_token") String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie.from(
                "refresh_token", ""
        )
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }
}
