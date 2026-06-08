package com.easybuy.cart_order.external.clients;

import com.easybuy.common.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "PRODUCT-CATEGORY-SERVICE")
public interface ProductClient {

    @GetMapping("/product/{productId}")
    ProductResponseDto getProductByProductId(@PathVariable UUID productId);

}
