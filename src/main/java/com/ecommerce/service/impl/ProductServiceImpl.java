package com.ecommerce.service.impl;

import com.ecommerce.domain.Category;
import com.ecommerce.domain.Product;
import com.ecommerce.dto.CreateProductRequest;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Product create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .imageUrl(request.getImageUrl())
                .sku(request.getSku())
                .manufacturer(request.getManufacturer())
                .category(category)
                .active(true)
                .build();
        return productRepository.save(product);
    }

    @Override
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    @Override
    public List<Product> getAllByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public Product update(Long id, CreateProductRequest request) {
        Product product = getById(id);
        
        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getManufacturer() != null) product.setManufacturer(request.getManufacturer());

        return productRepository.save(product);
    }

    @Override
    public void delete(Long id) {
        Product product = getById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public Product updateStock(Long id, int quantity) {
        Product product = getById(id);
        int newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        product.setStock(newStock);
        return productRepository.save(product);
    }
}
