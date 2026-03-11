package com.nithingodugu.ecommerce.productservice.repository;

import com.nithingodugu.ecommerce.common.contract.product.ProductPriceDetail;
import com.nithingodugu.ecommerce.productservice.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;


public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductId(String productId);

    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    List<Product> findByProductIdIn(Set<String> strings);
}