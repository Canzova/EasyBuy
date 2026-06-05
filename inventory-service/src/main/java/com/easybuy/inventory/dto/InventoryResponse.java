package com.easybuy.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse (
        long id,
        UUID productId,
        String sku,
        String warehouseLocation,
        int availableQuantity,
        int reservedQuantity,
        int totalQuantity,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt
){

}
