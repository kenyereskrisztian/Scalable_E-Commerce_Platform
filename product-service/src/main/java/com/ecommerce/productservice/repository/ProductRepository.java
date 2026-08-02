package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.domain.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Product> findByNameContainingIgnoreCase(String name);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll();
}
