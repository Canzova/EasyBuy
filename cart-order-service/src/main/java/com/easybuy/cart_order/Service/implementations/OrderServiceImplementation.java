package com.easybuy.cart_order.Service.implementations;
import com.easybuy.cart_order.Service.OrderService;
import com.easybuy.cart_order.dto.CheckoutRequest;
import com.easybuy.cart_order.dto.ItemResponse;
import com.easybuy.cart_order.dto.OrderResponse;
import com.easybuy.cart_order.dto.constants.CartStatus;
import com.easybuy.cart_order.dto.constants.OrderStatus;
import com.easybuy.cart_order.dto.constants.PaymentStatus;
import com.easybuy.cart_order.entity.Cart;
import com.easybuy.cart_order.entity.Item;
import com.easybuy.cart_order.entity.Order;
import com.easybuy.cart_order.entity.OrderItem;
import com.easybuy.cart_order.external.clients.InventoryClient;
import com.easybuy.cart_order.external.clients.ProductClient;
import com.easybuy.cart_order.repositories.CartRepository;
import com.easybuy.cart_order.repositories.OrderItemRepository;
import com.easybuy.cart_order.repositories.OrderRepository;
import com.easybuy.common.dto.InventoryResponse;
import com.easybuy.common.dto.ReleaseStock;
import com.easybuy.common.dto.ReserveStock;
import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.common.exceptions.customException.ResourceEmptyException;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImplementation implements OrderService {

    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderItemRepository orderItemRepository;


    @Override
    public OrderResponse checkout(UUID userId, CheckoutRequest checkoutRequest) {
        // Step 1 : Validate this userId
        log.info("Chckout started.");

        // Step 2 : Get the cart
        Cart cart = cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found."));
        log.info("Got cart for user {}", userId);

        if(cart.getItemList() == null || cart.getItemList().isEmpty()) throw new ResourceEmptyException("Cart is empty.");

        // Step 3 : Reserve the stocks in Inventory
        List<InventoryResponse> inventoryResponseList = new LinkedList<>();

        try{
            log.info("Started reserving the cart items.");
            for(Item item : cart.getItemList()){
                InventoryResponse inventoryResponse = inventoryClient.reserveByProductId(item.getProductId(), new ReserveStock(item.getQuantity()));
                inventoryResponseList.add(inventoryResponse);
            }

            Order order = buildOrderFromCart(cart, checkoutRequest);
            log.info("Got order {}.", order);

            // Clear the cart
            log.info("Updating the cart");
            cart.setCartStatus(CartStatus.CHECKED_OUT);
            cart.getItemList().clear();
            cart.setCheckOutAt(Instant.now());
            cart.setTotalPrice(BigDecimal.ZERO);
            cartRepository.save(cart);

            return orderToOrderResponse(order);
        }catch (Exception e){
            log.info("Something went wrong when trying to reserve the product for user : {} with exception : {}", userId, e.getMessage());

            for(int i = inventoryResponseList.size() - 1; i >= 0; i--){
                Item item = cart.getItemList().get(i);
                // Now release the stock of reserved stocks
                try{
                    inventoryClient.releaseByProductId(item.getProductId(), new ReleaseStock(item.getQuantity()));
                }catch (Exception ex){
                    log.info("Something went wrong when trying to release the product stock.");
                    throw new RuntimeException("Something went wrong when trying to release the product stock." + ex);
                }
            }

            throw new BusinessException("Checkout failed : " + e);
        }


    }

    private OrderResponse orderToOrderResponse(Order order) {
        log.info("Converting order to order response.");

        List<ItemResponse> itemResponseList = order.getOrderItemList().stream()
                        .map(item -> modelMapper.map(item, ItemResponse.class))
                                .toList();
        OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);
        orderResponse.setOrderItemList(itemResponseList);

        log.info("Conversion of Order to Order Response completed.");
        return orderResponse;
    }

    private Order buildOrderFromCart(Cart cart, CheckoutRequest checkoutRequest) {
        log.info("Building order from cart.");
        Order order = Order.builder()
                .shippingAddress(checkoutRequest.getShippingAddress())
                .billingName(checkoutRequest.getBillingName())
                .billingPhoneNumber(checkoutRequest.getBillingPhoneNumber())
                .orderNumber(UUID.randomUUID().toString())
                .paymentMethod(checkoutRequest.getPaymentMethod())
                .extraInfo(checkoutRequest.getExtraInformation())
                .orderNumber(UUID.randomUUID().toString())
                .userId(cart.getUserId())
                .billingPhoneNumber(checkoutRequest.getBillingPhoneNumber())
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(cart.getTotalPrice())
                .orderStatus(OrderStatus.CONFIRMED)
                .orderItemList(new ArrayList<>())
                .build();

        order.setOrderItemList(buildOrderItemListFromCartItemList(cart.getItemList(), order));

        // Also save into db
        order = orderRepository.save(order);

        log.info("Got order {}.", order);
        return order;
    }

    private List<OrderItem> buildOrderItemListFromCartItemList(List<Item> itemList, Order order) {
        log.info("Building order item list from cart.");
        return itemList.stream()
                .map(cartItem -> buildOrderItemFromCartItem(cartItem, order))
                .toList();

    }

    private OrderItem buildOrderItemFromCartItem(Item item, Order order) {
        log.info("Building order item from cart item.");
        OrderItem orderItem =  OrderItem.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountPercentage(item.getDiscountPercentage())
                .totalOrderItemPrice(item.getCartItemTotalPrice())
                .discountedPrice(item.getCartItemTotalPrice())
                .build();


        // Dp the mapping of order and orderItem
        orderItem.setOrder(order);
        order.getOrderItemList().add(orderItem);

        // Save into db
        orderItem = orderItemRepository.save(orderItem);

        log.info("Got order item {}.", orderItem);
        return orderItem;
    }
}
