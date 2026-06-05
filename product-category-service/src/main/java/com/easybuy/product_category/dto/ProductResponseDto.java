package com.easybuy.product_category.dto;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDto {

    private UUID id;

    private String title;
    private String shortDescription;


    private String longDescription;
    private BigDecimal price;

    private BigDecimal discount;
    private Boolean isLive = false;

    private List<String> productImages = new ArrayList<>();

    // As i am using DTO's no circular dependency will occur
    private List<CategoryResponseDto> categories = new ArrayList<>();
}
