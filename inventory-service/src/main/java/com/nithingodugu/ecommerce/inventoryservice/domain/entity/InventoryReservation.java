package com.nithingodugu.ecommerce.inventoryservice.domain.entity;

import com.nithingodugu.ecommerce.inventoryservice.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservationItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public void addItem(String productId, Integer quantity) {
        ReservationItem item = new ReservationItem();
        item.setReservation(this);
        item.setProductId(productId);
        item.setQuantity(quantity);
        this.items.add(item);
    }

}
