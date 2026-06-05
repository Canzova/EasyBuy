package com.easybuy.product_category.controller;

import com.easybuy.product_category.configuration.AppConstants;
import com.easybuy.product_category.dto.CategoryRequestDto;
import com.easybuy.product_category.dto.CategoryResponseDto;
import com.easybuy.product_category.dto.PageCategoryResponseDto;
import com.easybuy.product_category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/create")
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto categoryRequestDto){
        CategoryResponseDto categoryResponseDto = categoryService.createCategory(categoryRequestDto);
        return new ResponseEntity<>(categoryResponseDto, HttpStatus.CREATED);
    }

    @GetMapping()
    public ResponseEntity<PageCategoryResponseDto> getAllCategories(@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
                                                                    @RequestParam(name = "pageNum", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNum,
                                                                    @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder,
                                                                    @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORY_BY) String sortBy
                                                                    ){
        PageCategoryResponseDto pageCategoryResponseDto = categoryService.getAllCategories(pageSize, pageNum, sortOrder, sortBy);
        return new ResponseEntity<>(pageCategoryResponseDto, HttpStatus.OK);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable("categoryId") Long categoryId){
        CategoryResponseDto categoryResponseDto = categoryService.getCategoryById(categoryId);
        return new ResponseEntity<>(categoryResponseDto, HttpStatus.OK);
    }

    @PutMapping("/edit/{categoryId}")
    public ResponseEntity<CategoryResponseDto> updateCategoryById(@PathVariable("categoryId") Long categoryId, @Valid @RequestBody CategoryRequestDto categoryRequestDto){
        CategoryResponseDto categoryResponseDto = categoryService.updateCategoryById(categoryId, categoryRequestDto);
        return new ResponseEntity<>(categoryResponseDto, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{categoryId}")
    public ResponseEntity<CategoryResponseDto> deleteCategory(@PathVariable("categoryId") Long categoryId){
        CategoryResponseDto categoryResponseDto = categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(categoryResponseDto, HttpStatus.OK);
    }


}
