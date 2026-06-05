package com.easybuy.inventory.web;

import com.easybuy.inventory.domain.InventoryItem;
import com.easybuy.inventory.dto.*;
import com.easybuy.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest createInventoryRequest){
        InventoryResponse response = inventoryService.createInventory(createInventoryRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory(){
        List<InventoryResponse>inventoryResponseList = inventoryService.getAllInventory();
        return new ResponseEntity<>(inventoryResponseList, HttpStatus.OK);
    }

    @GetMapping("/{inventoryId}")

    public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable Long inventoryId){
        InventoryResponse inventoryResponse = inventoryService.getInventoryById(inventoryId);
        return new ResponseEntity<>(inventoryResponse, HttpStatus.OK);
    }

    @GetMapping("sku/{sku}")
    public ResponseEntity<InventoryResponse> getInventoryBySku(@PathVariable String sku){
        InventoryResponse response = inventoryService.getInventoryBySku(sku);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable UUID productId){
        InventoryResponse inventoryResponse = inventoryService.getInventoryByProductId(productId);
        return new ResponseEntity<>(inventoryResponse, HttpStatus.OK);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getInventoryByLowStock(@RequestParam(defaultValue = "4") @Min(0) Integer lowStock){
        List<InventoryResponse> inventoryResponseList = inventoryService.getInventoryByLowStock(lowStock);
        return new ResponseEntity<>(inventoryResponseList, HttpStatus.OK);
    }

    @PatchMapping("/{inventoryId}/adjust-stock")
    public ResponseEntity<InventoryResponse> adjustStockById(@PathVariable Long inventoryId,@Valid @RequestBody AdjustStockRequest adjustStockRequest){
        InventoryResponse inventoryResponse = inventoryService.adjustStockById(inventoryId, adjustStockRequest);
        return new ResponseEntity<>(inventoryResponse, HttpStatus.OK);
    }

    @PostMapping("/{inventoryId}/reserve")
    public ResponseEntity<InventoryResponse> reserveInventoryByInventoryId(@PathVariable Long inventoryId,@Valid @RequestBody ReserveStock reserveStock){
        InventoryResponse inventoryResponse = inventoryService.reserveInventoryByInventoryId(inventoryId, reserveStock);
        return new ResponseEntity<>(inventoryResponse, HttpStatus.OK);
    }

    @PostMapping("/{inventoryId}/release")
    public ResponseEntity<InventoryResponse> releaseInventoryByInventoryId(@PathVariable Long inventoryId, @Valid @RequestBody ReleaseStock releaseStock){
        InventoryResponse inventoryResponse = inventoryService.releaseInventoryByInventoryId(inventoryId, releaseStock);
        return new ResponseEntity<>(inventoryResponse, HttpStatus.OK);
    }

    @PutMapping("/{inventoryId}/update")
    public ResponseEntity<InventoryResponse> updateInventoryByInventoryId(@PathVariable Long inventoryId, @Valid @RequestBody UpdateInventoryRequest updateInventoryRequest){
        InventoryResponse inventoryResponse = inventoryService.updateInventoryByInventoryId(inventoryId, updateInventoryRequest);
        return new ResponseEntity<>(inventoryResponse, HttpStatus.OK);
    }

    @PostMapping("/product/{productId}/reserve")
    public InventoryResponse reserveByProductId(@PathVariable UUID productId, @Valid @RequestBody ReserveStock request) {
        return inventoryService.reserveStockByProductId(productId, request);
    }

    @PostMapping("/product/{productId}/release")
    public InventoryResponse releaseByProductId(@PathVariable UUID productId, @Valid @RequestBody ReleaseStock request) {
        return inventoryService.releaseStockByProductId(productId, request);
    }

    @DeleteMapping("/{inventoryId}/delete")
    public ResponseEntity<Void> deleteInventoryByInventoryId(@PathVariable Long inventoryId){
        inventoryService.deleteInventoryByInventoryId(inventoryId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
