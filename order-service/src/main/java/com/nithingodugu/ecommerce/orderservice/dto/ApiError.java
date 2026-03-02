package com.nithingodugu.ecommerce.orderservice.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ApiError {
    private LocalDateTime timeStamp;
    private String error;

    @Enumerated(EnumType.ORDINAL)
    private HttpStatus statusCode;

    public ApiError(){
        this.timeStamp = LocalDateTime.now();
    }

    public ApiError(String error, HttpStatus statusCode){
        this();
        this.error = error;
        this.statusCode = statusCode;
    }
}
