package com.easybuy.product_category.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDto {

    @NotBlank(message = "Product name cannot be blank.")
    @Size(min = 3, message = "Product name should have at least 3 characters.")
    private String title;

    private String shortDescription;

    private String longDescription;

    @NotNull
    @DecimalMin(value = "1.0", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;


    private BigDecimal discount;
    private Boolean isLive = false;

    @NotBlank
    private String categoryTitle;

}
