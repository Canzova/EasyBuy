package com.easybuy.product_category.service;

import com.easybuy.product_category.dto.CategoryRequestDto;
import com.easybuy.product_category.dto.CategoryResponseDto;
import com.easybuy.product_category.dto.PageCategoryResponseDto;

public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto);


    PageCategoryResponseDto getAllCategories(Integer pageSize, Integer pageNum, String sortOrder, String sortBy);

    CategoryResponseDto getCategoryById(Long categoryId);

    CategoryResponseDto updateCategoryById(Long categoryId, CategoryRequestDto categoryRequestDto);

    CategoryResponseDto deleteCategory(Long categoryId);
}
