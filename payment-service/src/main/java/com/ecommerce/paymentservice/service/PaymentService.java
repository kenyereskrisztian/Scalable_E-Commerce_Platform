package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResult;

public interface PaymentService {
    PaymentResult processPayment(Long orderId, PaymentRequest request);
}
