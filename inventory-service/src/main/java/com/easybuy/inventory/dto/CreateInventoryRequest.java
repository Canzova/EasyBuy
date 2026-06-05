package com.easybuy.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateInventoryRequest (

        @NotBlank(message = "SKU cannot be null or blank.")String sku,
        @NotNull(message = "Product Id cannot be null.") UUID productId,
        @NotBlank(message = "Product name cannot be null or blank.")String productName,
        @NotBlank(message = "Warehouse Location cannot be null or blank.")String warehouseLocation,
        @NotNull @Min(1) Integer availableQuantity,
        @Min(0) Integer reservedQuantity,
        @Min(0) Integer reorderLevel,
        Boolean active
){
}
