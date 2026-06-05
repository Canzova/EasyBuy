package com.easybuy.inventory.service;

import com.easybuy.inventory.domain.InventoryItem;
import com.easybuy.inventory.dto.*;
import com.easybuy.inventory.repository.InventoryRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImplementation implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public InventoryResponse createInventory(CreateInventoryRequest createInventoryRequest) {
        String sku = normalizeSku(createInventoryRequest.sku());


        // Check if this SKU is already assigned to other inventory item
        if(inventoryRepository.existsBySku(sku)) throw new RuntimeException("sku already exists : " + sku);

        // Check if product id is associated to a different inventory
        if(inventoryRepository.existsByProductId(createInventoryRequest.productId())) throw new RuntimeException("product already exists :  " + createInventoryRequest.productId());

        InventoryItem inventoryItem = createInventoryRequestToInventoryItem(createInventoryRequest);
        inventoryItem.setSku(sku);

        inventoryItem = inventoryRepository.save(inventoryItem);
        return inventoryItemToInventoryResponse(inventoryItem);

    }

    @Override
    public List<InventoryResponse> getAllInventory() {
        List<InventoryItem> inventoryResponseList = inventoryRepository.findAll();

        return inventoryResponseList.stream()
                .map(this::inventoryItemToInventoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long inventoryId) {
        InventoryItem inventoryItem = inventoryRepository.findById(inventoryId).orElseThrow(() -> new RuntimeException("inventory not found"));
        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryBySku(String sku) {
        InventoryItem inventoryItem = inventoryRepository.findBySku(sku).orElseThrow(()-> new RuntimeException("Inventory does not exists with sku : " + sku));
        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(UUID productId) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(productId).orElseThrow(()-> new RuntimeException("Inventory does not exists with productId : " + productId));
        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    public List<InventoryResponse> getInventoryByLowStock(Integer lowStock) {
        List<InventoryItem> inventoryItemsList = inventoryRepository.findByAvailableQuantityLessThanEqualAndActiveTrueOrderByAvailableQuantityAsc(lowStock);
        return inventoryItemsList.stream()
                .map(this::inventoryItemToInventoryResponse)
                .toList();
    }

    @Override
    public InventoryResponse adjustStockById(Long inventoryId, AdjustStockRequest adjustStockRequest) {
        InventoryItem inventoryItem = inventoryRepository.findByIdForUpdate(inventoryId).orElseThrow(() -> new RuntimeException("inventory does not exists with inventoryId : " + inventoryId));
        int newStock = safeInt(inventoryItem.getAvailableQuantity()) + adjustStockRequest.deltaStock();

        if(newStock < 0) throw new RuntimeException("Invalid delta-stock amount : " + adjustStockRequest.deltaStock());

        inventoryItem.setAvailableQuantity(newStock);
        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    public InventoryResponse reserveInventoryByInventoryId(Long inventoryId, @Valid ReserveStock reserveStock) {
        InventoryItem inventoryItem = inventoryRepository.findByIdForUpdate(inventoryId).orElseThrow(() -> new RuntimeException("inventory does not exists with inventoryId : " + inventoryId));

        int reserveQuantity = safeInt(reserveStock.quantity());
        int availableQuantity = inventoryItem.getAvailableQuantity();

        int newAvailableQuantity = (availableQuantity - reserveQuantity);
        if(newAvailableQuantity < 0) throw new RuntimeException("Invalid reserve-level amount : " + reserveQuantity);

        inventoryItem.setAvailableQuantity(newAvailableQuantity);
        inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() + reserveQuantity);
        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    public InventoryResponse releaseInventoryByInventoryId(Long inventoryId, @Valid ReleaseStock releaseStock) {
        InventoryItem inventoryItem = inventoryRepository.findByIdForUpdate(inventoryId).orElseThrow(() -> new RuntimeException("inventory does not exists with inventoryId : " + inventoryId));

        int reserveQuantityRequested = safeInt(releaseStock.quantity());
        int reserveQuantityAvailable = inventoryItem.getReservedQuantity();

        if(reserveQuantityRequested < 0 || reserveQuantityRequested > reserveQuantityAvailable)
            throw new RuntimeException("Invalid release-level amount : " + reserveQuantityRequested);

        inventoryItem.setReservedQuantity(reserveQuantityAvailable -  reserveQuantityRequested);
//        inventoryItem.setAvailableQuantity(inventoryItem.getAvailableQuantity() - reserveQuantityRequested);

        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    public InventoryResponse updateInventoryByInventoryId(Long inventoryId, UpdateInventoryRequest updateInventoryRequest) {
        InventoryItem inventoryItem = inventoryRepository.findByIdForUpdate(inventoryId).orElseThrow(() -> new RuntimeException("inventory does not exists with inventoryId : " + inventoryId));

        inventoryItem.setProductName(updateInventoryRequest.productName());
        inventoryItem.setActive(updateInventoryRequest.active());
        inventoryItem.setReorderLevel(updateInventoryRequest.reorderLevel());
        inventoryItem.setWarehouseLocation(updateInventoryRequest.warehouseLocation());

        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    public InventoryResponse reserveStockByProductId(UUID productId, ReserveStock reserveStock) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(productId).orElseThrow(() -> new RuntimeException("inventory does not exists with inventoryId : " + productId));

        int reserveQuantity = safeInt(reserveStock.quantity());
        int availableQuantity = inventoryItem.getAvailableQuantity();

        int newAvailableQuantity = (availableQuantity - reserveQuantity);
        if(reserveQuantity < 0 || newAvailableQuantity < 0) throw new RuntimeException("Invalid reserve-level amount : " + reserveQuantity);

        inventoryItem.setAvailableQuantity(newAvailableQuantity);
        inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() + reserveQuantity);
        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    public InventoryResponse releaseStockByProductId(UUID productId, ReleaseStock releaseStock) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(productId).orElseThrow(() -> new RuntimeException("inventory does not exists with inventoryId : " + productId));

        int reserveQuantityRequested = safeInt(releaseStock.quantity());
        int reserveQuantityAvailable = inventoryItem.getReservedQuantity();

        if(reserveQuantityRequested < 0 || reserveQuantityRequested > reserveQuantityAvailable)
            throw new RuntimeException("Invalid release-level amount : " + reserveQuantityRequested);

        inventoryItem.setReservedQuantity(reserveQuantityAvailable -  reserveQuantityRequested);
//        inventoryItem.setAvailableQuantity(inventoryItem.getAvailableQuantity() - reserveQuantityRequested);

        return inventoryItemToInventoryResponse(inventoryItem);
    }

    @Override
    public void deleteInventoryByInventoryId(Long inventoryId) {
        InventoryItem inventoryItem = inventoryRepository.findById(inventoryId).orElseThrow(() -> new RuntimeException("inventory not found"));
        inventoryRepository.delete(inventoryItem);
    }

    private int safeInt(Integer availableQuantity) {
        return availableQuantity == null ? 0 : availableQuantity;
    }

    private InventoryItem createInventoryRequestToInventoryItem(CreateInventoryRequest createInventoryRequest) {
        return new InventoryItem(
                createInventoryRequest.productId(),
                createInventoryRequest.sku(),
                createInventoryRequest.productName(),
                createInventoryRequest.warehouseLocation(),
                createInventoryRequest.availableQuantity(),
                createInventoryRequest.reservedQuantity(),
                createInventoryRequest.reorderLevel(),
                createInventoryRequest.active()
        );
    }

    private InventoryResponse inventoryItemToInventoryResponse(InventoryItem inventoryItem) {
        return new InventoryResponse(
                inventoryItem.getId(),
                inventoryItem.getProductId(),
                inventoryItem.getSku(),
                inventoryItem.getWarehouseLocation(),
                inventoryItem.getAvailableQuantity(),
                inventoryItem.getReservedQuantity(),
                inventoryItem.getAvailableQuantity() + inventoryItem.getReservedQuantity(),
                (inventoryItem.getAvailableQuantity() < inventoryItem.getReorderLevel()),
                inventoryItem.getCreatedAt(),
                inventoryItem.getUpdatedAt()
        );
    }

    private String normalizeSku(String sku) {
        if(!StringUtils.hasText(sku)) throw new RuntimeException("sku cannot be empty");
        sku = sku.toUpperCase();
        return sku;
    }
}
