package com.easybuy.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
public class ProductCategoryFallback {

    @GetMapping("/product-category-service-fallback")
    public Mono<String>  productCategoryFallback(){
        return Mono.just("Product Category Service is down, try again later.");
    }

    @GetMapping("/user-service-fallback")
    public Mono<String>  userServiceFallback(){
        return Mono.just("User Service is down, try again later.");
    }

    @GetMapping("/inventory-service-fallback")
    public Mono<String>  inventoryServiceFallback(){
        return Mono.just("Inventory Service is down, try again later.");
    }
}
