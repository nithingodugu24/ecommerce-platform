package com.nithingodugu.ecommerce.orderservice.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdGenerator {

    public String generateOrderId(){

        //order id pattern change in future
        return UUID.randomUUID().toString();
    }
}
