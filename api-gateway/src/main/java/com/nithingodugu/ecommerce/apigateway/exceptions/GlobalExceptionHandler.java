package com.nithingodugu.ecommerce.apigateway.exceptions;

import com.nithingodugu.ecommerce.apigateway.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        403,
                        ex.getMessage(),
                        Instant.now()
                ));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex){

        if (ex instanceof ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode())
                    .body(new ApiError(
                            rse.getStatusCode().value(),
                            rse.getReason(),
                            Instant.now()
                    ));
        }

        log.error("Unexpected error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        500,
                        "Internal server error",
                        Instant.now()
                ));
    }
}