package com.nithingodugu.ecommerce.cartservice.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    private int quantity;

    @Column(nullable = false)
    private BigDecimal  priceSnapshot;

    @ManyToOne
    @JoinColumn(name = "cart_id",nullable = false)
    private Cart cart;
}
