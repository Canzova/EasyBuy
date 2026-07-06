package com.easybuy.inventory.external;

import com.easybuy.common.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "CART-ORDER-SERVICE")
public interface OrderClient {

    @GetMapping("/order/orderId/{orderId}")
    public ResponseEntity<OrderResponse> getOrderByOrderId(@PathVariable Long orderId);

}
