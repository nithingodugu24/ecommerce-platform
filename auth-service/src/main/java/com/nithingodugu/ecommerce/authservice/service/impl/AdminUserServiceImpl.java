package com.nithingodugu.ecommerce.authservice.service.impl;

import com.nithingodugu.ecommerce.authservice.Util.Mappers;
import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import com.nithingodugu.ecommerce.authservice.repository.UserRepository;
import com.nithingodugu.ecommerce.authservice.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final Mappers mappers;

    @Override
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {

        Page<User> users = userRepository.findAll(pageable);
        return users.map(mappers::mapUserToUserProfileResponse);
    }
}
