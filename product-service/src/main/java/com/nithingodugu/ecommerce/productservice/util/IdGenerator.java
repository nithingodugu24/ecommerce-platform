package com.nithingodugu.ecommerce.productservice.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdGenerator {

    public String generateProductId(){
        return UUID.randomUUID().toString();
    }
}
