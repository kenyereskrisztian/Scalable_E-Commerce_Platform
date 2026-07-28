package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.paymentservice.client.OrderServiceClient;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResult;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderServiceClient orderServiceClient;

    @Override
    public PaymentResult processPayment(Long orderId, PaymentRequest request) {
        orderServiceClient.getOrder(orderId);

        String transactionId = UUID.randomUUID().toString();
        log.info("Processing payment for order: {} with transaction: {}", orderId, transactionId);

        boolean success = true;

        return PaymentResult.builder()
                .success(success)
                .transactionId(transactionId)
                .message(success ? "Payment processed successfully" : "Payment failed")
                .orderId(orderId)
                .build();
    }
}
