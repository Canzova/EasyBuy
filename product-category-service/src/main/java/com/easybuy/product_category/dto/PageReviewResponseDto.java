package com.easybuy.product_category.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PageReviewResponseDto {
    private List<ReviewResponseDto> reviews = new ArrayList<>();
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
    private Boolean hasNext;
}
