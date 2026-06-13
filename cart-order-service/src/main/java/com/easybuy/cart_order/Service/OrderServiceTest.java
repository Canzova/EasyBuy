//package com.easybuy.cart_order.Service;
//
//import com.easybuy.cart_order.HttpInterface.HttpInterface;
//import com.easybuy.cart_order.OpenFeign.OpenFeignClient;
//import com.easybuy.cart_order.dto.CategoryResponseDto;
//import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
//import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
//import io.github.resilience4j.retry.annotation.Retry;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class OrderService {
//
//    private final RestTemplate restTemplate;
//    private final RestClient restClient;
//    private final OpenFeignClient openFeignClient;
//    private final HttpInterface httpInterface;
//
////    @Retry(name = "getCategoryRetry", fallbackMethod = "getCategoryRetryFallback")
//    @RateLimiter(name = "getCategoryRateLimiting", fallbackMethod = "rateLimiterFallback")
//    public CategoryResponseDto getCategories(Long categoryId) {
////        CategoryResponseDto response = restTemplate.
////                getForObject("http://localhost:8080/category/" + categoryId, CategoryResponseDto.class);
////
////        return response;
//
////        CategoryResponseDto response = restClient.get()
////                .uri("category/" + categoryId)
////                .retrieve()
////                .body(CategoryResponseDto.class);
////
////        return response;
//
////        return openFeignClient.getCategoryById(categoryId);
//
//
//        return openFeignClient.getCategoryById(categoryId);
//    }
//
////    public CategoryResponseDto getCategoryRetryFallback(Long categoryId, Throwable ex){
////        return null;
////    }
//
//    public CategoryResponseDto rateLimiterFallback(Long categoryId, Throwable ex) {
//        return CategoryResponseDto.builder().id(null).title("Too many requests, try later").build();
//    }
//
//}
