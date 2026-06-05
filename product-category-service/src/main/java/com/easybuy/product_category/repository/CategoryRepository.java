package com.easybuy.product_category.repository;

import com.easybuy.product_category.entity.Category;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByTitle(String title);
    Optional<Category> findByTitle(String categoryTitle);
}
