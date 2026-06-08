package com.easybuy.cart_order.controller;

import com.easybuy.cart_order.Service.CartService;
import com.easybuy.cart_order.dto.AddCartItemRequest;
import com.easybuy.cart_order.dto.CartItemResponse;
import com.easybuy.cart_order.dto.CartResponse;
import com.easybuy.cart_order.dto.UpdateCartItemRequest;
import com.easybuy.common.dto.ProductResponseDto;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor

public class CartController {

    private final CartService cartService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<CartResponse> getCartByUserId(@PathVariable UUID userId) {
        CartResponse cartResponse = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cartResponse);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<CartItemResponse> saveItemToCart(@PathVariable UUID userId, @Valid @RequestBody AddCartItemRequest cartItemRequest){
        CartItemResponse cartItemResponse = cartService.saveItemToCart(userId, cartItemRequest);
        return ResponseEntity.ok(cartItemResponse);
    }

    @PutMapping ("/user/{userId}/product/{productId}")
    public ResponseEntity<CartItemResponse> updateCartItem(@PathVariable UUID userId, @PathVariable UUID productId,
                                                           @Valid @RequestBody UpdateCartItemRequest updateCartItemRequest){
        CartItemResponse cartItemResponse = cartService.updateCartItem(userId, productId, updateCartItemRequest);
        return ResponseEntity.ok(cartItemResponse);
    }

    @DeleteMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<CartResponse> deleteCartItem(@PathVariable UUID userId, @PathVariable UUID productId){
        CartResponse cartResponse = cartService.deleteCartItem(userId, productId);
        return new ResponseEntity<>(cartResponse, HttpStatus.OK);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable UUID userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

}
