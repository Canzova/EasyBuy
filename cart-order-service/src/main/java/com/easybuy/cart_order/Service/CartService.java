package com.easybuy.cart_order.Service;

import com.easybuy.cart_order.dto.AddCartItemRequest;
import com.easybuy.cart_order.dto.CartItemResponse;
import com.easybuy.cart_order.dto.CartResponse;
import com.easybuy.cart_order.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;

import java.util.UUID;

public interface CartService {
    CartResponse getCartByUserId(UUID userId);

    CartItemResponse saveItemToCart(UUID userId, @Valid AddCartItemRequest cartItemRequest);

    CartItemResponse updateCartItem(UUID userId, UUID productId, @Valid UpdateCartItemRequest updateCartItemRequest);

    CartResponse deleteCartItem(UUID userId, UUID productId);

    void clearCart(UUID userId);
}
