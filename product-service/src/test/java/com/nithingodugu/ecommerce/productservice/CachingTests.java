package com.nithingodugu.ecommerce.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.Cacheable;

@SpringBootTest
public class CachingTests {

    @Test
    @Cacheable("test")
    public void testCache(){
        System.out.println("executed");
    }
}
