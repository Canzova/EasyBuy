package com.easybuy.product_category.controller;

import com.easybuy.product_category.configuration.AppConstants;
import com.easybuy.product_category.dto.PageProductResponseDto;
import com.easybuy.product_category.dto.ProductRequestDto;
import com.easybuy.product_category.dto.ProductResponseDto;
import com.easybuy.product_category.dto.ReviewResponseDto;
import com.easybuy.product_category.entity.Product;
import com.easybuy.product_category.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@RefreshScope
public class ProductController {

    private final ProductService productService;

    @Value(("${product.discount}"))
    private Integer tempProductDiscount;

    @PostMapping("/create")
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto product){
        ProductResponseDto productResponseDto = productService.createProduct(product);
        return new ResponseEntity<>(productResponseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PageProductResponseDto> getAllProduct(@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
                                                                @RequestParam(name = "pageNum", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNum,
                                                                @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder,
                                                                @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY) String sortBy){
        PageProductResponseDto pageProductResponseDto = productService.getAllProducts(pageSize, pageNum, sortOrder, sortBy);
        return new ResponseEntity<>(pageProductResponseDto, HttpStatus.OK);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductByProductId(@PathVariable UUID productId){
        ProductResponseDto productResponseDto = productService.getProductByProductId(productId);
        return new ResponseEntity<>(productResponseDto, HttpStatus.OK);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageProductResponseDto> getProductByCategoryId(@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
                                                                         @RequestParam(name = "pageNum", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNum,
                                                                         @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder,
                                                                         @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY) String sortBy,
                                                                         @PathVariable Long categoryId){
        PageProductResponseDto pageProductResponseDto = productService.getProductByCategoryId(pageSize, pageNum, sortOrder, sortBy, categoryId);
        return new ResponseEntity<>(pageProductResponseDto, HttpStatus.OK);
    }

    @PutMapping("/edit/{productId}")
    public ResponseEntity<ProductResponseDto> editProductByProductId(@PathVariable UUID productId, @Valid @RequestBody ProductRequestDto productIdDto){
        ProductResponseDto editedProduct = productService.editProductByproductId(productId, productIdDto);
        return new ResponseEntity<>(editedProduct, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<ProductResponseDto> deleteProductByProductId(@PathVariable UUID productId){
        ProductResponseDto deletedProduct = productService.deleteProductByProductId(productId);
        return new ResponseEntity<>(deletedProduct, HttpStatus.OK);
    }

    @PostMapping("/{productId}/add/category/{categoryId}")
    public ResponseEntity<ProductResponseDto> addProduct(@PathVariable UUID productId, @PathVariable Long categoryId){
        ProductResponseDto productWithAddedCategory = productService.addCategoryToProduct(productId, categoryId);
        return new ResponseEntity<>(productWithAddedCategory, HttpStatus.OK);
    }

    @DeleteMapping("/{productId}/delete/category/{categoryId}")
    public ResponseEntity<ProductResponseDto> deleteProduct(@PathVariable UUID productId, @PathVariable Long categoryId){
        ProductResponseDto productWithDeletedCategory = productService.deleteCategoryFromProduct(productId, categoryId);
        return new ResponseEntity<>(productWithDeletedCategory, HttpStatus.OK);
    }

    @PostMapping("/{productId}/image")
    public ResponseEntity<ProductResponseDto> uploadImage(@PathVariable UUID productId, @RequestParam("files") List<MultipartFile> files){
        ProductResponseDto productResponseDto = productService.uploadImage(productId, files);
        return new ResponseEntity<>(productResponseDto, HttpStatus.OK);
    }

    @GetMapping("/{productId}/images")
    public ResponseEntity<List<String>> getProductImages(@PathVariable UUID productId){
        List<String> imagesList = productService.getAllImages(productId);
        return new ResponseEntity<>(imagesList, HttpStatus.OK);
    }

    @GetMapping("/temp-prod-discount")
    public ResponseEntity<Integer> getTempProdDiscount(){
        return new ResponseEntity<>(tempProductDiscount, HttpStatus.OK);
    }

}
