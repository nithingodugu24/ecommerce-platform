package com.nithingodugu.ecommerce.orderservice.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ApiError {
    String message;
    HttpStatus statusCode;
    Map<String, String> errors;
    private LocalDateTime timeStamp;

    public ApiError(){
        this.timeStamp = LocalDateTime.now();
    }

    public ApiError(String message, HttpStatus statusCode){
        this();
        this.message = message;
        this.statusCode = statusCode;
    }

    public ApiError(String message, HttpStatus statusCode, Map<String, String> errors){
        this();
        this.message = message;
        this.statusCode = statusCode;
        this.errors = errors;
    }
}
