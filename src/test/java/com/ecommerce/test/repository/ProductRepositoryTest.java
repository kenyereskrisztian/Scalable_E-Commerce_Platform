package com.ecommerce.test.repository;

import com.ecommerce.domain.Category;
import com.ecommerce.domain.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(Category.builder()
                .name("Electronics")
                .active(true)
                .build());
    }

    @Test
    void findByCategoryId_shouldReturnProductsInCategory() {
        productRepository.save(Product.builder()
                .name("Laptop").price(BigDecimal.valueOf(999)).category(category).stock(10).active(true)
                .build());
        productRepository.save(Product.builder()
                .name("Phone").price(BigDecimal.valueOf(599)).category(category).stock(10).active(true)
                .build());

        List<Product> products = productRepository.findByCategoryId(category.getId());

        assertThat(products).hasSize(2);
    }

    @Test
    void findByNameContainingIgnoreCase_shouldMatchPartially() {
        productRepository.save(Product.builder()
                .name("MacBook Pro").price(BigDecimal.valueOf(1999)).category(category).stock(5).active(true)
                .build());

        List<Product> products = productRepository.findByNameContainingIgnoreCase("macbook");

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("MacBook Pro");
    }
}
