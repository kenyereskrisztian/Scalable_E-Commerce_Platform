package com.ecommerce.service;

import com.ecommerce.domain.Category;
import com.ecommerce.dto.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {
    Category create(CreateCategoryRequest request);
    List<Category> getAll();
    Category getById(Long id);
    Category update(Long id, CreateCategoryRequest request);
    void delete(Long id);
}
