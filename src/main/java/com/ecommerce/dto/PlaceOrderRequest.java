package com.ecommerce.dto;

import lombok.Data;

@Data
public class PlaceOrderRequest {
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String notes;
}
