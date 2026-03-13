package com.nithingodugu.ecommerce.paymentservice.exceptions;

public class DuplicateRefundException extends BaseException {
    public DuplicateRefundException(String message) {
        super(message);
    }
}
