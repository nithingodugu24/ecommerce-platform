package com.nithingodugu.ecommerce.productservice.repository;

import com.nithingodugu.ecommerce.productservice.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);
}