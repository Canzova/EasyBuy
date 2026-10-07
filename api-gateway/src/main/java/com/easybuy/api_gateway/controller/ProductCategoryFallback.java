package com.easybuy.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
public class ProductCategoryFallback {

    @RequestMapping("/product-category-service-fallback")
    public Mono<String>  productCategoryFallback(){
        return Mono.just("Product Category Service is down, try again later.");
    }

    @RequestMapping("/user-service-fallback")
    public Mono<String>  userServiceFallback(){
        return Mono.just("User Service is down, try again later.");
    }

    @RequestMapping("/inventory-service-fallback")
    public Mono<String>  inventoryServiceFallback(){
        return Mono.just("Inventory Service is down, try again later.");
    }

    @RequestMapping("/cart-order-service-fallback")
    public Mono<String> cartOrderServiceFallback(){
        return Mono.just("Cart Order Service is down, try again later.");
    }

    @RequestMapping("/payment-service-fallback")
    public Mono<String> paymentServiceFallback(){
        return Mono.just("Payment Service is down, try again later.");
    }
}
