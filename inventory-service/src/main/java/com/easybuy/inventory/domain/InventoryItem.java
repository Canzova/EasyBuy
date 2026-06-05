package com.easybuy.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "inventories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_sku", columnNames = "sku"),
        @UniqueConstraint(name = "uk_inventory_product_id", columnNames = "productId")
})
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 128)
    private UUID productId;

    @Column(nullable = false, length = 128,unique = true)
    private String sku;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 120)
    private String warehouseLocation;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Column(nullable = false)
    private Integer reorderLevel;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist(){
        Instant now = Instant.now();

        if(this.createdAt == null) createdAt = now;
        updatedAt = now;

        if(this.availableQuantity == null) availableQuantity = 0;
        if(this.reservedQuantity == null) reservedQuantity = 0;
        if(this.reorderLevel == null) reorderLevel = 0;
    }

    public InventoryItem(UUID productId, String sku, String productName, String warehouseLocation, Integer availableQuantity, Integer reservedQuantity, Integer reorderLevel, Boolean active) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.warehouseLocation = warehouseLocation;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.reorderLevel = reorderLevel;
        this.active = active;
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = Instant.now();
    }

}
