package com.easybuy.product_category.service;

import com.easybuy.product_category.dto.PageReviewResponseDto;
import com.easybuy.product_category.dto.ReviewRequestDto;
import com.easybuy.product_category.dto.ReviewResponseDto;

import java.util.UUID;

public interface ReviewService {
    ReviewResponseDto createReview(UUID productId, ReviewRequestDto reviewRequestDto);

    ReviewResponseDto deleteReview(UUID productId, Long reviewId);
    ReviewResponseDto deleteReview(Long reviewId);

    ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto reviewRequestDto);


    PageReviewResponseDto getAllReviewsByProductId(Integer pageSize, Integer pageNum, String sortOrder, String sortBy, UUID productId);
}
