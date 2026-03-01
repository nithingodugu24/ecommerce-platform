package com.nithingodugu.ecommerce.authservice.service.impl;


import com.nithingodugu.ecommerce.authservice.Util.Mappers;
import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import com.nithingodugu.ecommerce.authservice.dto.ChangePasswordRequest;
import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import com.nithingodugu.ecommerce.authservice.repository.UserRepository;
import com.nithingodugu.ecommerce.authservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.mapper.Mapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final Mappers mappers;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                ()-> new RuntimeException("User not found")
        );

        return mappers.mapUserToUserProfileResponse(user);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(
                ()-> new RuntimeException("User not found")
        );

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())){
            throw new BadCredentialsException("Invalid Old Password");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
