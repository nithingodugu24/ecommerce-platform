package com.nithingodugu.ecommerce.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, message = "Password must be min 8 charaacters.")
        String password
){}