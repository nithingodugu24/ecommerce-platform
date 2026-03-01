package com.nithingodugu.ecommerce.authservice.controller;

import com.nithingodugu.ecommerce.authservice.dto.ChangePasswordRequest;
import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import com.nithingodugu.ecommerce.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UsersController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse getProfile(
            ){
        return userService.getProfile((UUID) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal());
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
            ){
        userService.changePassword((UUID) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
