package com.easybuy.product_category.service.implementations;

import com.easybuy.product_category.dto.PageReviewResponseDto;
import com.easybuy.product_category.dto.ReviewRequestDto;
import com.easybuy.product_category.dto.ReviewResponseDto;
import com.easybuy.product_category.entity.Product;
import com.easybuy.product_category.entity.Review;
import com.easybuy.product_category.exceptions.customException.ResourceNotFoundException;
import com.easybuy.product_category.repository.ProductRepository;
import com.easybuy.product_category.repository.ReviewRepository;
import com.easybuy.product_category.service.ReviewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImplementation implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Override
    public ReviewResponseDto createReview(UUID productId, ReviewRequestDto reviewRequestDto) {
        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));
        Review reviewEntity = modelMapper.map(reviewRequestDto, Review.class);

        reviewEntity.setProduct(productEntity);
        productEntity.getReviews().add(reviewEntity);

        reviewEntity =  reviewRepository.save(reviewEntity);
        return modelMapper.map(reviewEntity, ReviewResponseDto.class);
    }

    @Override
    public ReviewResponseDto deleteReview(UUID productId, Long reviewId) {
        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));
        Review reviewEntity = reviewRepository.findById(reviewId).orElseThrow(()-> new ResourceNotFoundException("Review does not exists."));

        productEntity.getReviews().remove(reviewEntity);
        return  modelMapper.map(reviewEntity, ReviewResponseDto.class);
    }

    @Override
    public ReviewResponseDto deleteReview(Long reviewId) {
        Review reviewEntity = reviewRepository.findById(reviewId).orElseThrow(()-> new ResourceNotFoundException("Review does not exists."));

        reviewRepository.delete(reviewEntity);
        return  modelMapper.map(reviewEntity, ReviewResponseDto.class);
    }

    @Override
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto reviewRequestDto) {
        Review reviewEntity =  reviewRepository.findById(reviewId).orElseThrow(()-> new ResourceNotFoundException("Review does not exists."));
        reviewEntity.setComment(reviewRequestDto.getComment());
        reviewEntity.setTitle(reviewRequestDto.getTitle());
        reviewEntity.setRating(reviewRequestDto.getRating());

        return  modelMapper.map(reviewEntity, ReviewResponseDto.class);
    }

    @Override
    public PageReviewResponseDto getAllReviewsByProductId(Integer pageSize, Integer pageNum, String sortOrder, String sortBy, UUID productId) {
        Product productEntity = productRepository.findProductById(productId).orElseThrow(()-> new ResourceNotFoundException("Product does not exists."));

        Pageable pageable = getPageRequest(pageSize, pageNum, sortOrder, sortBy);

        Page<Review> reviewList = reviewRepository.findByProduct(productEntity, pageable);
        if(reviewList.isEmpty()) throw new ResourceNotFoundException("Reviews does not exists.");

        return getPageReviewResponseDto(reviewList);


    }

    private PageReviewResponseDto getPageReviewResponseDto(Page<Review> reviewList) {
        PageReviewResponseDto  pageReviewResponseDto = new PageReviewResponseDto();
        pageReviewResponseDto.setTotalPages(reviewList.getTotalPages());
        pageReviewResponseDto.setPageSize(reviewList.getSize());
        pageReviewResponseDto.setPageNumber(reviewList.getNumber());
        pageReviewResponseDto.setHasNext(!reviewList.isLast());

        List<ReviewResponseDto> reviewResponseDtoList = reviewList.getContent().stream()
                .map(review -> modelMapper.map(review, ReviewResponseDto.class))
                .toList();

        pageReviewResponseDto.setReviews(reviewResponseDtoList);
        return pageReviewResponseDto;
    }

    private Pageable getPageRequest(Integer pageSize, Integer pageNum, String sortOrder, String sortBy) {
        Sort sortByOrder = sortOrder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();

        // Step 2 : Create Page Request
        return PageRequest.of(pageNum, pageSize, sortByOrder);
    }


}
