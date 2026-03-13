package com.nithingodugu.ecommerce.inventoryservice.repository;

import com.nithingodugu.ecommerce.inventoryservice.domain.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {


    @Query("SELECT p FROM Inventory p WHERE p.productId = :productId AND status = 'ACTIVE'")
    Optional<Inventory> findByProductId(String productId);

    @Modifying
    @Query("""
    UPDATE Inventory i
    SET i.availableQuantity = i.availableQuantity - :qty,
        i.reservedQuantity = i.reservedQuantity + :qty
    WHERE i.productId = :productId
    AND status = 'ACTIVE'
    AND i.availableQuantity >= :qty
    """)
    Integer reserveStock(String productId, Integer qty);

    @Modifying
    @Query("""
    UPDATE Inventory i
    SET i.reservedQuantity = i.reservedQuantity - :qty,
        i.availableQuantity = i.availableQuantity + :qty
    WHERE i.productId = :productId
        """)
    int releaseStock(String productId, Integer qty);

    List<Inventory> findAllByProductIdIn(List<String> productIds);
}
