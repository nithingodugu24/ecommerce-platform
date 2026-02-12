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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
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
    public AuthTokens login(LoginRequest request){

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
                Instant.now().plusMillis(jwtService.getRefreshExpiration() * 1000)
        );

        refreshTokenRepository.save(refreshToken);


        return new AuthTokens(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                accessToken,
                jwtService.getAccessExpiration(),
                rawRefreshToken
        );
    }

    public AuthTokens refresh(String rawRefreshToken){

        String[] tokenParts = rawRefreshToken.split("\\.");
        if(tokenParts.length != 2){
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        String tokenId = tokenParts[0];
        String tokenSecret = tokenParts[1];

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenIdAndRevokedFalse(tokenId)
                .orElseThrow(() -> new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG));

        if(
                storedToken.isRevoked()
                        || storedToken.getExpiry().isBefore(Instant.now())
                        || !passwordEncoder.matches(tokenSecret, storedToken.getTokenHash())

        ){
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        //rotation
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(
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
                Instant.now().plusMillis(jwtService.getRefreshExpiration() * 1000)
        );
        refreshTokenRepository.save(newRefreshToken);

        return new AuthTokens(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                newAccessToken,
                jwtService.getAccessExpiration(),
                newRawRefreshToken
        );
    }


    @Override
    public void logout(String refreshToken) {

        String[] tokenParts = refreshToken.split("\\.");
        if(tokenParts.length != 2){
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG);
        }

        String tokenId = tokenParts[0];

        RefreshToken token = refreshTokenRepository
                .findByTokenIdAndRevokedFalse(tokenId)
                .orElseThrow(() -> new BadCredentialsException(INVALID_REFRESH_TOKEN_MSG));

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

}
