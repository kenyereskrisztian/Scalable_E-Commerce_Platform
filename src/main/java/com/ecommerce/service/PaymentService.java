package com.ecommerce.service;

import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResult;

public interface PaymentService {
    PaymentResult processPayment(Long orderId, PaymentRequest request);
}
