package com.nithingodugu.ecommerce.orderservice.domain.entity;

import com.nithingodugu.ecommerce.orderservice.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_number", columnList = "orderNumber")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String orderId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addOrderItem(OrderItem item){
        item.setOrder(this);
        orderItems.add(item);
    }


}
