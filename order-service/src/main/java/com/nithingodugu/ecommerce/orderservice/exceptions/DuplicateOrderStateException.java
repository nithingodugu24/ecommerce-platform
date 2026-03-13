package com.nithingodugu.ecommerce.orderservice.exceptions;

public class DuplicateOrderStateException extends BaseException {
    public DuplicateOrderStateException(String message) {
        super(message);
    }
}
