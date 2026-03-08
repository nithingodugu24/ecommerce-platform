package com.nithingodugu.ecommerce.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Builder
@Getter
public class ApiError {

    private int status;
    private String message;
    private List<FieldError> errors;
    private Instant timestamp;


    @Builder
    public record FieldError(
            String field,
            String message,
            Object rejectedValue
    ){}
}
