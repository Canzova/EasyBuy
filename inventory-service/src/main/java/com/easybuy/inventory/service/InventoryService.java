package com.easybuy.inventory.service;

import com.easybuy.inventory.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.UUID;

public interface InventoryService {
    InventoryResponse createInventory(@Valid CreateInventoryRequest createInventoryRequest);

    List<InventoryResponse> getAllInventory();

    InventoryResponse getInventoryById(Long inventoryId);

    InventoryResponse getInventoryBySku(String sku);

    InventoryResponse getInventoryByProductId(UUID productId);

    List<InventoryResponse> getInventoryByLowStock(@Min(0) Integer lowStock);

    InventoryResponse adjustStockById(Long inventoryId, AdjustStockRequest adjustStockRequest);

    InventoryResponse reserveInventoryByInventoryId(Long inventoryId, ReserveStock reserveStock);

    InventoryResponse releaseInventoryByInventoryId(Long inventoryId, ReleaseStock releaseStock);

    InventoryResponse updateInventoryByInventoryId(Long inventoryId, @Valid UpdateInventoryRequest updateInventoryRequest);

    InventoryResponse reserveStockByProductId(UUID productId, @Valid ReserveStock request);

    InventoryResponse releaseStockByProductId(UUID productId, @Valid ReleaseStock request);

    void deleteInventoryByInventoryId(Long inventoryId);
}
