package com.nithingodugu.ecommerce.authservice.service.impl;

import com.nithingodugu.ecommerce.authservice.domain.entity.RefreshToken;
import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserRole;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserStatus;
import com.nithingodugu.ecommerce.authservice.dto.*;
import com.nithingodugu.ecommerce.authservice.repository.RefreshTokenRepository;
import com.nithingodugu.ecommerce.authservice.repository.UserRepository;
import com.nithingodugu.ecommerce.authservice.security.jwt.JwtService;
import com.nithingodugu.ecommerce.authservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String BAD_CREDENTIALS_MSG = "Invalid email or password";
    private static final String ACCOUNT_INACTIVE_MSG = "Your account is not active";
    private static final String INVALID_REFRESH_TOKEN_MSG = "Invalid refresh token";

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if(userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("Email already exists");
        }

        String hashPassword = passwordEncoder.encode(request.password());

        User user = new User(
                request.email(),
                hashPassword,
                UserRole.USER
        );

        userRepository.save(user);

        return mapToResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request)     {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(()-> new BadCredentialsException(BAD_CREDENTIALS_MSG));

        if(user.getStatus() != UserStatus.ACTIVE){
            throw new DisabledException(ACCOUNT_INACTIVE_MSG);
        }

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())){
            throw new BadCredentialsException(BAD_CREDENTIALS_MSG);
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getRole().name()
        );

        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenValue,
                user,
                Instant.now().plusMillis(jwtService.getRefreshExpiration() * 1000)
        );

        refreshTokenRepository.save(refreshToken);


        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                accessToken,
                refreshTokenValue,
                jwtService.getAccessExpiration()
        );
    }

    public LoginResponse refresh(RefreshTokenRequest request){
        RefreshToken token = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG));

        System.out.println(request.refreshToken());
        System.out.println(token);
        System.out.println(token.getExpiry());

        if(token.isRevoked() || token.getExpiry().isBefore(Instant.now())){
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        User user = token.getUser();

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getRole().name()
        );

        return mapToLoginResponse(user, newAccessToken, request.refreshToken());
    }

    @Override
    public void logout(String refreshToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        token.revoke();
        refreshTokenRepository.save(token);
    }

    private RegisterResponse mapToResponse(User user){
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private LoginResponse mapToLoginResponse(User user, String accessToken, String refreshTokenValue){
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                accessToken,
                refreshTokenValue,
                jwtService.getAccessExpiration()
        );
    }
}
