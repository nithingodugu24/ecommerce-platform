package com.nithingodugu.ecommerce.productservice.repository;

import com.nithingodugu.ecommerce.productservice.domain.entity.Product;
import com.nithingodugu.ecommerce.productservice.dto.ProductPricingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    @Query("""
        SELECT new com.nithingodugu.ecommerce.productservice.dto.ProductPricingResponse(
               p.id,
               p.name,
               p.price,
               p.active
        )
        FROM Product p 
        WHERE p.id IN :ids
       """)
    List<ProductPricingResponse> findPricingByIds(@Param("ids") List<Long> ids);

}