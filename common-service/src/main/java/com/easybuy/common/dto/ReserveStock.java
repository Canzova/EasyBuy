package com.easybuy.common.dto;

import jakarta.validation.constraints.Min;

public record ReserveStock(
        @Min(0) Integer quantity
) {
}
