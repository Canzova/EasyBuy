package com.easybuy.cart_order.OpenFeign;

import com.easybuy.cart_order.dto.CategoryResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@FeignClient(name = "PRODUCT-CATEGORY-SERVICE")
@FeignClient(name = "PRODUCT-CATEGORY-SERVICE", fallback = Fallback.class)
public interface OpenFeignClient {

    @GetMapping("/category/{categoryId}")
    public CategoryResponseDto getCategoryById(@PathVariable Long categoryId);

}
