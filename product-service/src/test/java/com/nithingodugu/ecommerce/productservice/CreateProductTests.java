package com.nithingodugu.ecommerce.productservice;

import com.nithingodugu.ecommerce.productservice.dto.CreateProductRequestDto;
import com.nithingodugu.ecommerce.productservice.dto.ProductResponseDto;
import com.nithingodugu.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest
public class CreateProductTests {

    @Autowired
    private ProductService productService;

    @Test
    public void createSampleProduct(){
        CreateProductRequestDto request = new CreateProductRequestDto(
                "test phone",
                "test phone description",
                1005.00,
                "Mobiles",
                100
        );
        ProductResponseDto response =  productService.createProduct(request);
        System.out.println(response);
    }
}
