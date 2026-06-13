package com.easybuy.cart_order.Service;

import com.easybuy.cart_order.dto.AddItemRequest;
import com.easybuy.cart_order.dto.ItemResponse;
import com.easybuy.cart_order.dto.CartResponse;
import com.easybuy.cart_order.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;

import java.util.UUID;

public interface CartService {
    CartResponse getCartByUserId(UUID userId);

    ItemResponse saveItemToCart(UUID userId, @Valid AddItemRequest cartItemRequest);

    ItemResponse updateCartItem(UUID userId, UUID productId, @Valid UpdateCartItemRequest updateCartItemRequest);

    CartResponse deleteCartItem(UUID userId, UUID productId);

    void clearCart(UUID userId);
}
