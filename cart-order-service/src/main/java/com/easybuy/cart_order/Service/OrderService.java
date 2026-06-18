package com.easybuy.cart_order.Service;

import com.easybuy.cart_order.dto.CheckoutRequest;
import com.easybuy.cart_order.dto.OrderResponse;
import com.easybuy.cart_order.dto.constants.PaymentStatus;


import java.util.List;
import java.util.UUID;

public interface OrderService{
    OrderResponse checkout(UUID userId, CheckoutRequest checkoutRequest);

    OrderResponse getOrderByOrderId(Long orderId);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    List<OrderResponse> getAllOrdersOfUser(UUID userId);

    OrderResponse cancelOrder(Long orderId);

    void updateOrderStatus(Long orderId, String status);

}
