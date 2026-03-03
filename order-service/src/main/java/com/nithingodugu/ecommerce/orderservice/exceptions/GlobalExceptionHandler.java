package com.nithingodugu.ecommerce.orderservice.exceptions;

import com.nithingodugu.ecommerce.common.exceptions.InvalidProductException;
import com.nithingodugu.ecommerce.common.exceptions.OutOfStockException;
import com.nithingodugu.ecommerce.orderservice.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidProductException.class)
    public ResponseEntity<ApiError> handleInvalidProduct(InvalidProductException ex){
        ApiError apiError = new ApiError(
                 ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ApiError> handleOutOfStock(OutOfStockException ex){
        ApiError apiError = new ApiError(
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        ApiError apiError = new ApiError(
                "Validation failed",
                HttpStatus.BAD_REQUEST,
                errors
        );

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericError(Exception ex){
        log.error(ex.toString());
        ApiError apiError = new ApiError(
                "Something went wrong : " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }
}
