package com.easybuy.inventory.dto;

import jakarta.validation.constraints.Min;

public record AdjustStockRequest (
        @Min(value = 0, message = "Delta stock cannot be negative.") Integer deltaStock,
        String reason
){
}
