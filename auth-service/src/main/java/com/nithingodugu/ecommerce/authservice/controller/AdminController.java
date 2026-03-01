package com.nithingodugu.ecommerce.authservice.controller;

import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import com.nithingodugu.ecommerce.authservice.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(Pageable pageable){

        Page<UserProfileResponse> page = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(page);
    }
}
