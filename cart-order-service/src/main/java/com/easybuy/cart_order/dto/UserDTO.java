package com.easybuy.cart_order.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private UUID userId;

    @NotBlank(message = "Name cannot be blank or null.")
    @Size(min = 3, max = 25, message = "Name should have at-least 3 and at-max 25 characters.")
    private String name;

    @NotBlank(message = "Email cannot be blank or null.")
    @Email
    private String email;

    @NotBlank(message = "Password cannot be blank or null.")
    @Size(min = 5, message = "Password should have at-least 5 characters.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotBlank(message = "Address cannot be blank or null.")
    @Size(min = 5, max = 50, message = "Address should have at-least 5 and at-max 50 characters.")
    private String address;

    @NotBlank(message = "Phone cannot be blank or null.")
    @Size(min = 10, max = 10, message = "Phone number should be of exactly 10 digits")
    private String phone;
}

