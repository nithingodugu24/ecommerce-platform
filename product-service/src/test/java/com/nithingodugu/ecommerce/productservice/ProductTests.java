package com.nithingodugu.ecommerce.productservice;

import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@SpringBootTest
public class ProductTests {
    @Autowired
    private ProductService productService;

    @Test
    public void productTests(){
//        Page<ProductResponseDto> products = productService.getProductsByName("car", Pageable.ofSize(10));
//        for(ProductResponseDto product: products){
//            System.out.println(product);
//        }
    }
}
