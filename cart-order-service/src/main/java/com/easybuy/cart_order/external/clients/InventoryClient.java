package com.easybuy.cart_order.external.clients;

import com.easybuy.common.dto.InventoryResponse;
import com.easybuy.common.dto.ReleaseStock;
import com.easybuy.common.dto.ReserveStock;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient("INVENTORY-SERVICE")
public interface InventoryClient {

    @PostMapping("/api/inventories/product/{productId}/reserve")
    public InventoryResponse reserveByProductId(@PathVariable UUID productId, @RequestBody ReserveStock request);

    @PostMapping("/api/inventories/product/{productId}/release")
    public InventoryResponse releaseByProductId(@PathVariable UUID productId, @RequestBody ReleaseStock request);
}
