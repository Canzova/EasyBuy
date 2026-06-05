package com.easybuy.inventory.dto;

import jakarta.validation.constraints.Min;

public record ReserveStock(
        @Min(0) Integer quantity
) {
}
