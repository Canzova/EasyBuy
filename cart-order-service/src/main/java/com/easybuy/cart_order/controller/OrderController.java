//package com.easybuy.cart_order.controller;
//
//import com.easybuy.cart_order.Service.OrderService;
//import com.easybuy.cart_order.dto.CategoryResponseDto;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/order")
//@RefreshScope
//public class OrderController {
//
//    private final OrderService orderService;
//    @Value("${order.count}")
//    private Integer orderCount;
//
//    @GetMapping("/category/{categoryId}")
//    public ResponseEntity<CategoryResponseDto> getCategories(@PathVariable Long categoryId){
//        CategoryResponseDto categoryResponseList = orderService.getCategories(categoryId);
//        return new ResponseEntity<>(categoryResponseList, HttpStatus.OK);
//    }
//
//    @GetMapping("/order-count")
//    public ResponseEntity<Integer> orderCount(){
//        return new ResponseEntity<>(orderCount, HttpStatus.OK);
//    }
//
//}
