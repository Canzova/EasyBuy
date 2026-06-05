package com.easybuy.product_category.service.implementations;

import com.easybuy.product_category.dto.CategoryRequestDto;
import com.easybuy.product_category.dto.CategoryResponseDto;
import com.easybuy.product_category.dto.PageCategoryResponseDto;
import com.easybuy.product_category.entity.Category;
import com.easybuy.product_category.exceptions.customException.ResourceNotFoundException;
import com.easybuy.product_category.repository.CategoryRepository;
import com.easybuy.product_category.service.CategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoryServiceImplementation implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto) {
        // Step 1 : Check if this category is already created
        if(categoryRepository.existsByTitle(categoryRequestDto.getTitle())) throw new ResourceNotFoundException("Category already exists");

        // Step 2 : Get the entity
        Category categoryEntity = modelMapper.map(categoryRequestDto, Category.class);

        // Step 3 : Safe the entity
        categoryEntity = categoryRepository.save(categoryEntity);

        // Step 4 : Return the dto
        return modelMapper.map(categoryEntity, CategoryResponseDto.class);
    }

    @Override
    public PageCategoryResponseDto getAllCategories(Integer pageSize, Integer pageNum, String sortOrder, String sortBy) {
//        log.info("Inside getAllProducts....");
//        log.info("Retrying......");
//        if(2 < 5) throw new NullPointerException("Exception occured inside product-category-service");


        Pageable pageable = getPageable(pageSize, pageNum, sortOrder, sortBy);
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        return getPageCategoryResponseDto(categoryPage);
    }

    @Override
    public CategoryResponseDto getCategoryById(Long categoryId) {
//        log.info("I will retry");
//        if(2 < 5) throw new NullPointerException("Exception occurred while getting categories");

        Category categoryEntity = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category not found."));
        return modelMapper.map(categoryEntity, CategoryResponseDto.class);
    }

    @Override
    public CategoryResponseDto updateCategoryById(Long categoryId, CategoryRequestDto categoryRequestDto) {
        Category categoryEntity = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category not found."));
        categoryEntity.setTitle(categoryRequestDto.getTitle());
        return modelMapper.map(categoryEntity, CategoryResponseDto.class);
    }

    @Override
    public CategoryResponseDto deleteCategory(Long categoryId) {
        Category categoryEntity = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category not found."));
        categoryRepository.delete(categoryEntity);
        return modelMapper.map(categoryEntity, CategoryResponseDto.class);
    }

    private PageCategoryResponseDto getPageCategoryResponseDto(Page<Category> categoryPage) {
        List<CategoryResponseDto> categoryResponseDtoList = getCategoryResponseDto(categoryPage.getContent());

        PageCategoryResponseDto pageCategoryResponseDto = new PageCategoryResponseDto();
        pageCategoryResponseDto.setCategories(categoryResponseDtoList);
        pageCategoryResponseDto.setTotalPages(categoryPage.getTotalPages());
        pageCategoryResponseDto.setPageNumber(categoryPage.getNumber());
        pageCategoryResponseDto.setPageSize(categoryPage.getSize());
        pageCategoryResponseDto.setHasNext(!categoryPage.isLast());

        return pageCategoryResponseDto;
    }

    private List<CategoryResponseDto> getCategoryResponseDto(List<Category> content) {
        return content.stream()
                .map((category -> modelMapper.map(category, CategoryResponseDto.class)))
                .toList();
    }

    private Pageable getPageable(Integer pageSize, Integer pageNum, String sortOrder, String sortBy){
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();

        return PageRequest.of(pageNum, pageSize, sortByAndOrder);
    }
}
