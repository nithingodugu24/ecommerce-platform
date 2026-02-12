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
//        CreateProductDto p1 = new CreateProductDto(
//                "Apple 2025 MacBook Air M4 ",
//                "13-inch, Apple M4 chip with 10-core CPU and 8-core GPU, 16GB Unified Memory, 256GB",
//                92199.0,
//                "Computers & Accessories"
//        );
//
//        System.out.println(p1);
//
//        ProductResponseDto p11 = productService.createProduct(p1);
//        System.out.println(p11);


        Page<ProductResponseDto> products = productService.getProductsByName("car", Pageable.ofSize(10));
        for(ProductResponseDto product: products){
            System.out.println(product);
        }
    }
}
