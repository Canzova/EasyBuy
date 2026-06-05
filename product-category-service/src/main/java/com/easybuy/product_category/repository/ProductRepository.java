package com.easybuy.product_category.repository;

import com.easybuy.product_category.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @EntityGraph(attributePaths = {"categories"})
    Optional<Product> findProductById(UUID productId);

    Page<Product> findByCategories_Id(Long categoryId, Pageable pageRequest);

}