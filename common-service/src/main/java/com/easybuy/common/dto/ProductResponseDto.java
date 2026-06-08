package com.easybuy.common.dto;


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

}
