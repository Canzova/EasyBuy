package com.easybuy.inventory.repository;

import com.easybuy.inventory.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    boolean existsBySku(String sku);

    boolean existsByProductId(UUID productId);

    Optional<InventoryItem> findBySku(String sku);

    Optional<InventoryItem> findByProductId(UUID productId);

    List<InventoryItem> findByAvailableQuantityLessThanEqualAndActiveTrueOrderByAvailableQuantityAsc(Integer lowStock);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long inventoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.productId = :id")
    Optional<InventoryItem> findByProductIdForUpdate(@Param("id") Long productId);
}
