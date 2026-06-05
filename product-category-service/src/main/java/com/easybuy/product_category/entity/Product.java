package com.easybuy.product_category.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseEntity{

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

    @OneToMany(mappedBy = "product",  fetch = FetchType.LAZY,  cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 25)
    private Set<Review> reviews = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "products")
    @BatchSize(size = 25)
    private Set<Category> categories = new HashSet<>();

}
