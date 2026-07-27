package com.ecommerce.test.controller;

import com.ecommerce.controller.ProductController;
import com.ecommerce.domain.Category;
import com.ecommerce.domain.Product;
import com.ecommerce.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void search_shouldFilterByCategory() throws Exception {
        Product product = Product.builder()
                .id(1L).name("Laptop").price(BigDecimal.valueOf(999)).stock(5).active(true)
                .category(Category.builder().id(1L).build())
                .build();

        when(productService.getAllByCategory(1L)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products?categoryId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void search_shouldFilterByName() throws Exception {
        Product product = Product.builder()
                .id(1L).name("MacBook Pro").price(BigDecimal.valueOf(1999)).stock(3).active(true)
                .category(Category.builder().id(1L).build())
                .build();

        when(productService.searchByName("mac")).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products?name=mac"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("MacBook Pro"));
    }

    @Test
    void getById_shouldReturnProduct() throws Exception {
        Product product = Product.builder()
                .id(1L).name("Phone").price(BigDecimal.valueOf(599)).stock(10).active(true)
                .category(Category.builder().id(1L).build())
                .build();

        when(productService.getById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phone"));
    }
}
