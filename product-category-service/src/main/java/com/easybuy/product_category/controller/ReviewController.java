package com.easybuy.product_category.controller;

import com.easybuy.product_category.configuration.AppConstants;
import com.easybuy.product_category.dto.PageReviewResponseDto;
import com.easybuy.product_category.dto.ReviewRequestDto;
import com.easybuy.product_category.dto.ReviewResponseDto;
import com.easybuy.product_category.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/add/product/{productId}")
    public ResponseEntity<ReviewResponseDto> createReview(@PathVariable UUID productId, @Valid @RequestBody ReviewRequestDto reviewRequestDto){
        ReviewResponseDto reviewResponseDto = reviewService.createReview(productId, reviewRequestDto);
        return new ResponseEntity<>(reviewResponseDto, HttpStatus.CREATED);
    }

    @DeleteMapping("{reviewId}/delete/product/{productId}")
    public ResponseEntity<ReviewResponseDto> deleteReview(@PathVariable UUID productId, @PathVariable Long reviewId){
        ReviewResponseDto reviewResponseDto = reviewService.deleteReview(productId, reviewId);
        return new ResponseEntity<>(reviewResponseDto, HttpStatus.CREATED);
    }

    @PutMapping("/update/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewRequestDto reviewRequestDto){
        ReviewResponseDto updatedReview = reviewService.updateReview(reviewId, reviewRequestDto);
        return new ResponseEntity<>(updatedReview, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<ReviewResponseDto> deleteReview(@PathVariable Long reviewId){
        ReviewResponseDto deletedReview = reviewService.deleteReview(reviewId);
        return new ResponseEntity<>(deletedReview, HttpStatus.OK);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageReviewResponseDto> getAllReviewsByProductId(@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
                                                                          @RequestParam(name = "pageNum", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNum,
                                                                          @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder,
                                                                          @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY) String sortBy,
                                                                          @PathVariable UUID productId){
        PageReviewResponseDto reviewResponseDto = reviewService.getAllReviewsByProductId(pageSize, pageNum, sortOrder, sortBy, productId);
        return new ResponseEntity<>(reviewResponseDto, HttpStatus.OK);
    }
}
