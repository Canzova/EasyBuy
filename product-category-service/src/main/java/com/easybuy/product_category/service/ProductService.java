package com.easybuy.product_category.service;

import com.easybuy.product_category.dto.PageProductResponseDto;
import com.easybuy.product_category.dto.ProductRequestDto;
import com.easybuy.product_category.dto.ProductResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto product);
    PageProductResponseDto getAllProducts(Integer pageSize, Integer pageNum, String sortOrder, String sortBy);

    ProductResponseDto getProductByProductId(UUID productId);

    PageProductResponseDto getProductByCategoryId(Integer pageSize, Integer pageNum, String sortOrder, String sortBy, Long categoryId);

    ProductResponseDto editProductByproductId(UUID productId, ProductRequestDto productIdDto);

    ProductResponseDto deleteProductByProductId(UUID productId);

    ProductResponseDto addCategoryToProduct(UUID productId, Long categoryId);

    ProductResponseDto deleteCategoryFromProduct(UUID productId, Long categoryId);

    ProductResponseDto uploadImage(UUID productId, List<MultipartFile> files);

    List<String> getAllImages(UUID productId);

}
