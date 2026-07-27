package com.ecommerce.service;

import com.ecommerce.dto.incoming.PaymentRequest;
import com.ecommerce.dto.outgoing.PaymentResult;

public interface PaymentService {
    PaymentResult processPayment(Long orderId, PaymentRequest request);
}
