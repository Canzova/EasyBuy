package com.easybuy.cart_order.Service.implementations;

import com.easybuy.cart_order.Service.CartService;
import com.easybuy.cart_order.dto.*;
import com.easybuy.cart_order.dto.constants.CartStatus;
import com.easybuy.cart_order.entity.Cart;
import com.easybuy.cart_order.entity.Item;
import com.easybuy.cart_order.external.clients.ProductClient;
import com.easybuy.cart_order.external.clients.UserClient;
import com.easybuy.cart_order.repositories.CartItemRepository;
import com.easybuy.cart_order.repositories.CartRepository;
import com.easybuy.common.dto.ProductResponseDto;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartServiceImplementation implements CartService {

    private final CartRepository cartRepository;
    private final ModelMapper modelMapper;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final CartItemRepository cartItemRepository;

    @Override
    public CartResponse getCartByUserId(UUID userId) {
        UserDTO userDTO = getUserFromId(userId);
        Cart cart = getCartFromUserId(userId);
        return getCartResponse(cart);
    }

    private @NonNull CartResponse getCartResponse(Cart cart) {
        List<ItemResponse> itemResponse = cart.getItemList().stream()
                .map(item -> modelMapper.map(item, ItemResponse.class))
                .toList();

        CartResponse cartResponse = modelMapper.map(cart, CartResponse.class);
        cartResponse.setCartItemList(itemResponse);
        return cartResponse;
    }

    private Cart getCartFromUserId(UUID userId) {
       return cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .userId(userId)
                            .itemList(new ArrayList<>())
                            .cartStatus(CartStatus.ACTIVE)
                            .totalPrice(new BigDecimal(0))
                            .build();

                    return cartRepository.save(newCart);
                });
    }

    private UserDTO getUserFromId(UUID userId) {
        try{
            return userClient.getUserByUserId(userId);
        }catch (Exception e){
            throw new ResourceNotFoundException("User not found");
        }
    }

    @Override
    public ItemResponse saveItemToCart(UUID userId, AddItemRequest cartItemRequest) {
        UserDTO user = getUserFromId(userId);
        Cart cart = getCartFromUserId(userId);
        ProductResponseDto productResponseDto = getProduct(cartItemRequest.getProductId());

        // Check if this product is already present into cart then increment its count
        Item existingItem = cart.getItemList().stream()
                .filter(existingProduct -> existingProduct.getProductId().equals(productResponseDto.getId()))
                .findFirst()
                .orElseGet(()->{
                    Item newItem = Item.builder()
                            .cart(cart)
                            .quantity(0)
                            .productId(productResponseDto.getId())
                            .build();

                    cart.getItemList().add(newItem);
                    return newItem;
                });

        existingItem.setQuantity(existingItem.getQuantity() + cartItemRequest.getQuantity());
        existingItem.setProductName(productResponseDto.getTitle());
        existingItem.setUnitPrice(productResponseDto.getPrice());
        existingItem.setDiscountedPrice(calculateFinalUnitPrice(productResponseDto.getPrice(), productResponseDto.getDiscount()));
        existingItem.setDiscountPercentage(productResponseDto.getDiscount().intValue());
        existingItem.setCartItemTotalPrice(existingItem.getDiscountedPrice().multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())));

        cart.setTotalPrice(computeCartTotalPrice(cart));

        existingItem = cartItemRepository.save(existingItem);
        return modelMapper.map(existingItem, ItemResponse.class);
    }

    private static  BigDecimal computeCartTotalPrice(Cart cart) {
        return cart.getItemList().stream()
                .map(Item::getCartItemTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public ItemResponse updateCartItem(UUID userId, UUID productId, UpdateCartItemRequest updateCartItemRequest) {
        UserDTO user = getUserFromId(userId);
        Cart cart = getCartFromUserId(userId);  // In persistent state

       Item item = cart.getItemList().stream()
                .filter(existingProduct -> existingProduct.getProductId().equals(productId))
                .findFirst()
               .orElseThrow(()-> new ResourceNotFoundException("Product Not found into cart."));

       item.setQuantity(updateCartItemRequest.getQuantity());
       item.setCartItemTotalPrice(item.getDiscountedPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
       cartItemRepository.save(item);

       return modelMapper.map(item, ItemResponse.class);
    }

    @Override
    public CartResponse deleteCartItem(UUID userId, UUID productId) {
        UserDTO user = getUserFromId(userId);
        Cart cart = getCartFromUserId(userId);

        // Step 1 : get the cartItem which has this product and delete it
        Item cartItem = cart.getItemList().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(()-> new ResourceNotFoundException("Product Not found into cart."));

        // You have orphan removal as true for cart so when you just remove it from cartItemList then hibernate will automatically deletes it
        cart.getItemList().remove(cartItem);

        // Step 2 : You also need to update the total price in cart
        cart.setTotalPrice(computeCartTotalPrice(cart));

        return getCartResponse(cart);
    }

    @Override
    public void clearCart(UUID userId) {
        UserDTO user = getUserFromId(userId);
        Cart cart = getCartFromUserId(userId);
        cartRepository.delete(cart);
    }

    // Used scale to make sure that if a non terminating remainder is found used last 2 digits after .(dot) and use HALF_UP --> Standard for money
    private BigDecimal calculateFinalUnitPrice(BigDecimal price, BigDecimal discount) {
        BigDecimal discountedPrice = discount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(price);
        return price.subtract(discountedPrice);
    }

    private ProductResponseDto getProduct(UUID productId) {
        try{
            ProductResponseDto productResponseDto = productClient.getProductByProductId(productId);

            if(productResponseDto == null || productResponseDto.getIsLive() == null || !productResponseDto.getIsLive()) {
                throw new ResourceNotFoundException("Product not found");
            }

            return productResponseDto;
        } catch (RuntimeException e) {
            log.debug(e.getMessage());
            throw new ResourceNotFoundException("Product not found");
        }
    }
}
