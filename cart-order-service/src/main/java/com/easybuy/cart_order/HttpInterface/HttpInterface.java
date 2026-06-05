package com.easybuy.cart_order.HttpInterface;

import com.easybuy.cart_order.dto.CategoryResponseDto;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

// With this you can use both RestClient or webflux
@HttpExchange(url = "PRODUCT-CATEGORY-SERVICE")
public interface HttpInterface {

    @GetExchange("/category/{id}")
    CategoryResponseDto getCategoryById(Long id);

}
