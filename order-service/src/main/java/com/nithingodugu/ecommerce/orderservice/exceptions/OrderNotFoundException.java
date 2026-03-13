package com.nithingodugu.ecommerce.orderservice.exceptions;

public class OrderNotFoundException extends BaseException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
