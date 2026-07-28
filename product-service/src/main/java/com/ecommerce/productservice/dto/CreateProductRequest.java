package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull @Positive
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private String sku;
    private String manufacturer;
    @NotNull
    private Long categoryId;
}
