package com.ecommerce.productservice.service;

import com.ecommerce.productservice.domain.Category;
import com.ecommerce.productservice.dto.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {
    Category create(CreateCategoryRequest request);
    List<Category> getAll();
    Category getById(Long id);
    Category update(Long id, CreateCategoryRequest request);
    void delete(Long id);
}
