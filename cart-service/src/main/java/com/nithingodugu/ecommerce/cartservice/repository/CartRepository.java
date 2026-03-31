package com.nithingodugu.ecommerce.cartservice.repository;

import com.nithingodugu.ecommerce.cartservice.domain.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(String userId);
}