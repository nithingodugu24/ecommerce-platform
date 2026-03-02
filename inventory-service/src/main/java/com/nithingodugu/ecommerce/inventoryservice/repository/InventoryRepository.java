package com.nithingodugu.ecommerce.inventoryservice.repository;

import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(long productId);

    @Modifying
    @Query("""
    UPDATE Inventory i
    SET i.availableQuantity = i.availableQuantity - :qty,
        i.reservedQuantity = i.reservedQuantity + :qty
    WHERE i.productId = :productId
      AND i.availableQuantity >= :qty
""")
    Integer reserveStock(String productId, Integer qty);
}
