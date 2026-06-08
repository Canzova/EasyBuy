//package com.easybuy.cart_order.OpenFeign;
//
//import com.easybuy.cart_order.dto.CategoryResponseDto;
//import org.springframework.stereotype.Component;
//
//@Component
//public class Fallback implements OpenFeignClient {
//
//    @Override
//    public CategoryResponseDto getCategoryById(Long categoryId) {
//        return  CategoryResponseDto.builder()
//                .id(null)
//                .title("Dummy result")
//                .build();
//    }
//}
