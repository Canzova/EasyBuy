package com.easybuy.cart_order.controller;

import com.easybuy.cart_order.Service.OrderService;
import com.easybuy.cart_order.dto.CheckoutRequest;
import com.easybuy.cart_order.dto.OrderResponse;
import com.easybuy.cart_order.dto.OrderResponse;
import com.easybuy.common.dto.ReserveStock;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/user/{userId}/checkout")
    public ResponseEntity<OrderResponse> checkout(@PathVariable("userId") UUID userId, @Valid @RequestBody CheckoutRequest checkoutRequest){
        OrderResponse orderResponse = orderService.checkout(userId, checkoutRequest);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/orderId/{orderId}")
    public ResponseEntity<OrderResponse> getOrderByOrderId(@PathVariable Long orderId){
        OrderResponse orderResponse = orderService.getOrderByOrderId(orderId);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/orderNumber/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(@PathVariable String orderNumber){
        OrderResponse orderResponse = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getAllOrdersOfUser(@PathVariable UUID userId){
        List<OrderResponse> orderResponseList = orderService.getAllOrdersOfUser(userId);
        return ResponseEntity.ok(orderResponseList);
    }

    @DeleteMapping("/order/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        OrderResponse deletedOrder = orderService.cancelOrder(orderId);
        return new ResponseEntity<>(deletedOrder,  HttpStatus.OK);
    }

}
