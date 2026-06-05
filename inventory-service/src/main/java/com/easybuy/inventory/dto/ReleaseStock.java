package com.easybuy.inventory.dto;


import jakarta.validation.constraints.Min;

public record ReleaseStock(
        @Min(0) Integer quantity
) {
}
