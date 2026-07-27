package com.ecommerce.service;

import com.ecommerce.domain.Product;
import com.ecommerce.dto.CreateProductRequest;

import java.util.List;

public interface ProductService {
    List<Product> getAll();
    Product create(CreateProductRequest request);
    Product getById(Long id);
    List<Product> getAllByCategory(Long categoryId);
    List<Product> searchByName(String name);
    Product update(Long id, CreateProductRequest request);
    void delete(Long id);
    Product updateStock(Long id, int quantity);
}
