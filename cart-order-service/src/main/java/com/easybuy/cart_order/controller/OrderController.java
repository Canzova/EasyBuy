package com.easybuy.cart_order.controller;

import com.easybuy.cart_order.Service.OrderService;
import com.easybuy.cart_order.dto.CheckoutRequest;
import com.easybuy.cart_order.dto.OrderRequest;
import com.easybuy.cart_order.dto.OrderResponse;
import com.easybuy.common.dto.ReserveStock;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/user/{userId}/checkout")
    public ResponseEntity<OrderResponse> checkout(@PathVariable("userId") UUID userId, @Valid @RequestBody CheckoutRequest checkoutRequest){
        OrderResponse orderResponse = orderService.checkout(userId, checkoutRequest);
        return ResponseEntity.ok(orderResponse);
    }

}
