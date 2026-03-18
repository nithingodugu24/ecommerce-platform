package com.nithingodugu.ecommerce.authservice.service.impl;

import com.nithingodugu.ecommerce.authservice.domain.entity.RefreshToken;
import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserRole;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserStatus;
import com.nithingodugu.ecommerce.authservice.dto.*;
import com.nithingodugu.ecommerce.authservice.dto.AuthTokens;
import com.nithingodugu.ecommerce.authservice.dto.LoginRequest;
import com.nithingodugu.ecommerce.authservice.dto.RegisterRequest;
import com.nithingodugu.ecommerce.authservice.dto.RegisterResponse;
import com.nithingodugu.ecommerce.authservice.repository.RefreshTokenRepository;
import com.nithingodugu.ecommerce.authservice.repository.UserRepository;
import com.nithingodugu.ecommerce.authservice.security.jwt.JwtUtil;
import com.nithingodugu.ecommerce.authservice.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;

    private static final String BAD_CREDENTIALS_MSG = "Invalid email or password";
    private static final String ACCOUNT_INACTIVE_MSG = "Your account is not active";
    private static final String INVALID_REFRESH_TOKEN_MSG = "Invalid refresh token";

    @Override
    public RegisterResponse register(RegisterRequest request) {

        log.info("Register attempt", kv("email", request.email()));

        if(userRepository.existsByEmail(request.email())){

            log.warn("Register Failed",
                    kv("email", request.email()),
                    kv("reason", "ACCOUNT_ALREADY_EXISTS")
            );

            throw new IllegalArgumentException("Email already exists");
        }

        String hashPassword = passwordEncoder.encode(request.password());

        User user = new User(
                request.email(),
                hashPassword,
                UserRole.USER
        );

        userRepository.save(user);

        log.info("Register success",
                kv("userId", user.getId()),
                kv("role", user.getRole())
        );

        return mapToResponse(user);
    }

    @Override
    public AuthTokens login(LoginRequest request){

        log.info("Login attempt", kv("email", request.email()));

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        }catch (Exception ex){
            log.warn("Login Failed",
                    kv("email", request.email()),
                    kv("reason", "INVALID_CREDENTIALS")
            );

            throw ex;
        }

        User user = (User) authentication.getPrincipal();

        if(user.getStatus() != UserStatus.ACTIVE){

            log.warn("Login Failed",
                    kv("email", request.email()),
                    kv("reason", "ACCOUNT_INACTIVE")
            );

            throw new DisabledException(ACCOUNT_INACTIVE_MSG);
        }

        log.info("Login success",
                kv("userId", user.getId()),
                kv("role", user.getRole())
        );

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getRole().name()
        );

        //raw token (sent in cookie)
        String tokenId = UUID.randomUUID().toString();
        String tokenSecret = UUID.randomUUID().toString();

        String rawRefreshToken = tokenId + "." + tokenSecret;

        //hashed token (stores in db)
        String hashedSecret = passwordEncoder.encode(tokenSecret);

        RefreshToken refreshToken = new RefreshToken(
                tokenId,
                hashedSecret,
                user,
                Instant.now().plusMillis(jwtUtil.getRefreshExpiration() * 1000)
        );

        refreshTokenRepository.save(refreshToken);

        log.info("Tokens issued",
                kv("userId", user.getId())
        );

        return new AuthTokens(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                accessToken,
                jwtUtil.getAccessExpiration(),
                rawRefreshToken
        );
    }

    public AuthTokens refresh(String rawRefreshToken){

        log.debug("Refresh request");

        String[] tokenParts = rawRefreshToken.split("\\.");
        if(tokenParts.length != 2){

            log.warn("Refresh failed",
                    kv("reason", "INVALID_FORMAT"));

            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        String tokenId = tokenParts[0];
        String tokenSecret = tokenParts[1];

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenIdAndRevokedFalse(tokenId)
                .orElseThrow(() -> {
                    log.warn("Refresh failed",
                            kv("tokenId", tokenId),
                            kv("reason", "TOKEN_NOT_FOUND"));
                    return new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
                });

        if(storedToken.isRevoked()){

            log.warn("Refresh failed",
                    kv("tokenId", tokenId),
                    kv("reason", "TOKEN_REVOKED"));

            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        if(storedToken.getExpiry().isBefore(Instant.now())){

            log.warn("Refresh failed",
                    kv("tokenId", tokenId),
                    kv("reason", "TOKEN_EXPIRED"));

            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        if(!passwordEncoder.matches(tokenSecret, storedToken.getTokenHash())){

            log.warn("Refresh failed",
                    kv("tokenId", tokenId),
                    kv("reason", "TOKEN_MISMATCH"));

            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        //rotation
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getRole().name()
        );

        String newTokenId = UUID.randomUUID().toString();
        String newTokenSecret = UUID.randomUUID().toString();

        String newRawRefreshToken = newTokenId + "." + newTokenSecret;

        //hashed token (stores in db)
        String hashedSecret = passwordEncoder.encode(newTokenSecret);

        RefreshToken newRefreshToken = new RefreshToken(
                newTokenId,
                hashedSecret,
                user,
                Instant.now().plusMillis(jwtUtil.getRefreshExpiration() * 1000)
        );
        refreshTokenRepository.save(newRefreshToken);

        log.info("Refresh success",
                kv("userId", user.getId()),
                kv("oldTokenId", tokenId),
                kv("newTokenId", newTokenId)
        );

        return new AuthTokens(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                newAccessToken,
                jwtUtil.getAccessExpiration(),
                newRawRefreshToken
        );
    }

    @Override
    public void logout(String refreshToken) {

        log.debug("Logout attempt");

        String[] tokenParts = refreshToken.split("\\.");
        if(tokenParts.length != 2){

            log.warn("Logout failed", kv("reason", "INVALID_FORMAT"));

            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        String tokenId = tokenParts[0];

        RefreshToken token = refreshTokenRepository
                .findByTokenIdAndRevokedFalse(tokenId)
                .orElseThrow(() -> {
                    log.warn("Logout failed",
                            kv("tokenId", tokenId),
                            kv("reason", "TOKEN_NOT_FOUND"));
                    return new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
                });

        token.revoke();
        refreshTokenRepository.save(token);

        log.info("Logout success",
                kv("userId", token.getUser().getId()),
                kv("tokenId", tokenId));
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

}
