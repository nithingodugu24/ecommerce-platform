package com.nithingodugu.ecommerce.common.exceptions;

public class OutOfStockException extends RuntimeException{
    public OutOfStockException(String message){
        super(message);
    }
}
