package com.easybuy.cart_order.Service;

import com.easybuy.cart_order.dto.CheckoutRequest;
import com.easybuy.cart_order.dto.OrderRequest;
import com.easybuy.cart_order.dto.OrderResponse;

import java.util.UUID;

public interface OrderService{
    OrderResponse checkout(UUID userId, CheckoutRequest checkoutRequest);
}
