package com.nithingodugu.ecommerce.authservice.service.impl;


import com.nithingodugu.ecommerce.authservice.Util.Mappers;
import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import com.nithingodugu.ecommerce.authservice.dto.ChangePasswordRequest;
import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import com.nithingodugu.ecommerce.authservice.repository.UserRepository;
import com.nithingodugu.ecommerce.authservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.mapper.Mapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final Mappers mappers;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getProfile(UUID userId) {

        log.debug("Get Profile attempt");

        User user = userRepository.findById(userId).orElseThrow(
                ()-> {
                    log.warn("Get profile failed",
                            kv("reason", "USER_NOT_FOUND")
                    );
                    return  new RuntimeException("User not found");
                }
        );

        log.debug("Get profile success");

        return mappers.mapUserToUserProfileResponse(user);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {

        log.info("Change Password attempt");

        User user = userRepository.findById(userId).orElseThrow(
                ()-> {
                    log.warn("Change Password failed",
                            kv("reason", "USER_NOT_FOUND")
                    );
                    return new RuntimeException("User not found");
                }
        );

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())){
            log.warn("Change Password failed",
                    kv("reason", "PASSWORD_MISMATCH")
            );
            throw new BadCredentialsException("Invalid Old Password");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Change Password success");
    }
}
