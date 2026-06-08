package com.easybuy.inventory.external;

import com.easybuy.inventory.dto.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "PRODUCT-CATEGORY-SERVICE")
public interface ProductClient {

    @GetMapping("/product/{productId}")
    Product getProductById(@PathVariable UUID productId);

}
