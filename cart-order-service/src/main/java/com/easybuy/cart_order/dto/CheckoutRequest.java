package com.easybuy.cart_order.dto;

import com.easybuy.cart_order.dto.constants.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {
    @NotBlank(message = "Billing name cannot be null or blank") String billingName;
    @NotBlank(message = "Billing phone cannot be null or blank") String billingPhoneNumber;
    @NotBlank(message = "Shipping Address cannot be null or blank") String shippingAddress;
    PaymentMethod paymentMethod;
    String extraInformation;
}
