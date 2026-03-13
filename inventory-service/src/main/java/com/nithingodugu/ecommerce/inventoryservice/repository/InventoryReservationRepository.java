package com.nithingodugu.ecommerce.inventoryservice.repository;

import com.nithingodugu.ecommerce.inventoryservice.domain.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByOrderId(String orderId);
}