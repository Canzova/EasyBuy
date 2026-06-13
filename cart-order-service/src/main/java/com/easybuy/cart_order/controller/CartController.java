package com.easybuy.cart_order.controller;

import com.easybuy.cart_order.Service.CartService;
import com.easybuy.cart_order.dto.AddItemRequest;
import com.easybuy.cart_order.dto.ItemResponse;
import com.easybuy.cart_order.dto.CartResponse;
import com.easybuy.cart_order.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;
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
    public ResponseEntity<ItemResponse> saveItemToCart(@PathVariable UUID userId, @Valid @RequestBody AddItemRequest cartItemRequest){
        ItemResponse itemResponse = cartService.saveItemToCart(userId, cartItemRequest);
        return ResponseEntity.ok(itemResponse);
    }

    @PutMapping ("/user/{userId}/product/{productId}")
    public ResponseEntity<ItemResponse> updateCartItem(@PathVariable UUID userId, @PathVariable UUID productId,
                                                       @Valid @RequestBody UpdateCartItemRequest updateCartItemRequest){
        ItemResponse itemResponse = cartService.updateCartItem(userId, productId, updateCartItemRequest);
        return ResponseEntity.ok(itemResponse);
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
