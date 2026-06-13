package com.easybuy.product_category.service.implementations;

import com.easybuy.common.exceptions.customException.ImageUploadFailedException;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import com.easybuy.product_category.dto.*;
import com.easybuy.product_category.entity.Category;
import com.easybuy.product_category.entity.Product;
import com.easybuy.product_category.repository.CategoryRepository;
import com.easybuy.product_category.repository.ProductRepository;
import com.easybuy.product_category.service.ImageStorageService;
import com.easybuy.product_category.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImplementation implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService  imageStorageService;
    private final ModelMapper modelMapper;

    @Override
    public ProductResponseDto createProduct(ProductRequestDto productDto) {
        // Step 1 : Check if category exists
        Category categoryEntity = categoryRepository.findByTitle(productDto.getCategoryTitle())
                .orElseThrow(()->new ResourceNotFoundException("Category does not exists."));


        // Step 2 : Get the entity
        Product productEntity = modelMapper.map(productDto, Product.class);

        // Step 3 : Create the relationship in memory
        categoryEntity.getProducts().add(productEntity);
        productEntity.getCategories().add(categoryEntity);
        productEntity.setIsLive(true);

        // Save the product Entity, category is in persistence context so it will be saved during flush
        // Aso you have cascase.persist and cascade.merge so category will be saved first into db
        productEntity = productRepository.save(productEntity);

        // Step 4 : Return the dto
        return generateProductResponseDto(productEntity);
    }

    @Override
    public PageProductResponseDto getAllProducts(Integer pageSize, Integer pageNum, String sortOrder, String sortBy) {

//        log.info("Inside getAllProducts....");
//        log.info("Retrying......");
//        if(2 < 5) throw new NullPointerException("Exception occured inside product-category-service");

        // Step 1 : Create Sort object
        Pageable pageable = getPageRequest(pageSize, pageNum, sortOrder, sortBy);

        // Step 3 : Get the page
        Page<Product> page = productRepository.findAll(pageable);
        
        // Step 4 : get the products
        return getPageProductResponseDto(page);
    }

    @Override
    public ProductResponseDto getProductByProductId(UUID productId) {
        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));
        return generateProductResponseDto(productEntity);
    }

    @Override
    public PageProductResponseDto getProductByCategoryId(Integer pageSize, Integer pageNum, String sortOrder, String sortBy, Long categoryId) {

        Pageable pageable = getPageRequest(pageSize, pageNum, sortOrder, sortBy);

        Page<Product> productListPage = productRepository.findByCategories_Id(categoryId, pageable);
        if(productListPage.getContent().isEmpty()) throw new ResourceNotFoundException("Product does not exists in this category.");

        return getPageProductResponseDto(productListPage);

    }

    @Override
    public ProductResponseDto editProductByproductId(UUID productId, ProductRequestDto productIdDto) {
        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));
        productEntity.setTitle(productIdDto.getTitle());
        productEntity.setDiscount(productIdDto.getDiscount());
        productEntity.setPrice(productIdDto.getPrice());
        productEntity.setShortDescription(productIdDto.getShortDescription());
        productEntity.setLongDescription(productIdDto.getLongDescription());
        productEntity.setIsLive(productIdDto.getIsLive());

        return generateProductResponseDto(productEntity);
    }

    @Override
    public ProductResponseDto deleteProductByProductId(UUID productId) {
        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));
        productRepository.delete(productEntity);
        return generateProductResponseDto(productEntity);
    }

    @Override
    public ProductResponseDto addCategoryToProduct(UUID productId, Long categoryId) {
        Category categoryEntity = categoryRepository.findById(categoryId)
                .orElseThrow(()->new ResourceNotFoundException("Category does not exists"));

        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));

        categoryEntity.getProducts().add(productEntity);
        productEntity.getCategories().add(categoryEntity);

        return generateProductResponseDto(productEntity);
    }

    @Override
    public ProductResponseDto deleteCategoryFromProduct(UUID productId, Long categoryId) {
        Category categoryEntity = categoryRepository.findById(categoryId)
                .orElseThrow(()->new ResourceNotFoundException("Category does not exists"));

        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));

        categoryEntity.getProducts().remove(productEntity);
        productEntity.getCategories().remove(categoryEntity);
        return generateProductResponseDto(productEntity);
    }

    @Override
    public ProductResponseDto uploadImage(UUID productId, List<MultipartFile> files) {
        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));

        for(MultipartFile file : files){
            String url = null;
            try {
                url = imageStorageService.uploadImages(file);
            } catch (IOException e) {
                throw new ImageUploadFailedException("Image upload failed.");
            }
            productEntity.getProductImages().add(url);
        }

        return generateProductResponseDto(productEntity);
    }

    @Override
    public List<String> getAllImages(UUID productId) {

        Product product = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists with given productId"));
        List<String>images = product.getProductImages();

        if(images.isEmpty()) throw new ResourceNotFoundException("Product does not have any image.");
        return images;
    }


    private PageProductResponseDto getPageProductResponseDto(Page<Product> page) {
        List<Product>productList = page.getContent();

        PageProductResponseDto pageProductResponseDto = new PageProductResponseDto();
        pageProductResponseDto.setPageSize(page.getNumberOfElements());
        pageProductResponseDto.setPageNumber(page.getNumber());
        pageProductResponseDto.setTotalPages(page.getTotalPages());
        pageProductResponseDto.setHasNext(!page.isLast());

        List<ProductResponseDto> productResponseDtoList = new ArrayList<>();
        productList.forEach((product) ->{
            productResponseDtoList.add(generateProductResponseDto(product));
        });

        pageProductResponseDto.setProducts(productResponseDtoList);
        return pageProductResponseDto;
    }


    private ProductResponseDto generateProductResponseDto(Product product) {
        List<CategoryResponseDto>categoryResponseDtoList  = product.getCategories().stream()
                .map((category) -> modelMapper.map(category, CategoryResponseDto.class))
                .toList();

        ProductResponseDto productResponseDto = modelMapper.map(product, ProductResponseDto.class);
        productResponseDto.setCategories(categoryResponseDtoList);


        return productResponseDto;
    }

    private Pageable getPageRequest(Integer pageSize, Integer pageNum, String sortOrder, String sortBy) {

        Sort sortByOrder = Sort.unsorted();

       if(sortBy != null && !sortBy.isEmpty()){
           sortByOrder = sortOrder.equalsIgnoreCase("asc") ?
                   Sort.by(sortBy).ascending() :
                   Sort.by(sortBy).descending();
       }

        // Step 2 : Create Page Request
        return PageRequest.of(pageNum, pageSize, sortByOrder);
    }
}
