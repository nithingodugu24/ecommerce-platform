package com.nithingodugu.ecommerce.authservice.service;

import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<UserProfileResponse> getAllUsers(Pageable pageable);
}
