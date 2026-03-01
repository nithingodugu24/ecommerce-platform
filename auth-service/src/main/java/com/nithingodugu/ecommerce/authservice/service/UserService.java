package com.nithingodugu.ecommerce.authservice.service;

import com.nithingodugu.ecommerce.authservice.dto.ChangePasswordRequest;
import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import jakarta.validation.Valid;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfile(UUID userId);

    void changePassword(UUID userId, @Valid ChangePasswordRequest request);
}
