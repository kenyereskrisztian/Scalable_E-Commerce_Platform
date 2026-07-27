package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotBlank
    private String cardNumber;
    @NotBlank
    private String cardHolder;
    @NotBlank
    private String expiryDate;
    @NotBlank
    private String cvv;
    @NotNull @Positive
    private BigDecimal amount;
}
