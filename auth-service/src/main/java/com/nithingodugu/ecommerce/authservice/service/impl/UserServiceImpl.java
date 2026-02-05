package com.nithingodugu.ecommerce.authservice.service.impl;

import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserRole;
import com.nithingodugu.ecommerce.authservice.domain.enums.UserStatus;
import com.nithingodugu.ecommerce.authservice.dto.LoginRequest;
import com.nithingodugu.ecommerce.authservice.dto.LoginResponse;
import com.nithingodugu.ecommerce.authservice.dto.RegisterRequest;
import com.nithingodugu.ecommerce.authservice.dto.RegisterResponse;
import com.nithingodugu.ecommerce.authservice.repository.UserRepository;
import com.nithingodugu.ecommerce.authservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String BAD_CREDENTIALS_MSG = "Invalid email or password";
    private static final String ACCOUNT_INACTIVE_MSG = "Your account is not active";

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

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
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
