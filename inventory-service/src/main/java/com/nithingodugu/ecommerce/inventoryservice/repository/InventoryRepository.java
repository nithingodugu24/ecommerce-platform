package com.nithingodugu.ecommerce.inventoryservice.repository;

import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(long productId);
}
