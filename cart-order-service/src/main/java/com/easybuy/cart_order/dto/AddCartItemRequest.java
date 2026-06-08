package com.easybuy.cart_order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddCartItemRequest {

    @NotNull(message = "Product is required.")
    private UUID productId;
    @Min(value = 1, message = "Minimum one quantity is required")
    private Integer quantity;

}
