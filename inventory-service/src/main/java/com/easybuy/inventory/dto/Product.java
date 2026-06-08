package com.easybuy.inventory.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    private String shortDescription;

    @Lob
    private String longDescription;
    private BigDecimal price;

    private BigDecimal discount;
    private Boolean isLive;

    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 5)
    private List<String>productImages = new ArrayList<>();

}
