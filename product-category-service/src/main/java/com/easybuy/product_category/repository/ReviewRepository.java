package com.easybuy.product_category.repository;

import com.easybuy.product_category.entity.Product;
import com.easybuy.product_category.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProduct(Product productEntity, Pageable pageable);
}
