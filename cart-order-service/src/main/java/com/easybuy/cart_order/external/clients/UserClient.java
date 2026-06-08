package com.easybuy.cart_order.external.clients;

import com.easybuy.cart_order.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "USER-SERVICE")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    public UserDTO getUserByUserId(@PathVariable UUID userId);

}
