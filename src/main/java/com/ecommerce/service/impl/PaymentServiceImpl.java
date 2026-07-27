package com.ecommerce.service.impl;

import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResult;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;

    @Override
    public PaymentResult processPayment(Long orderId, PaymentRequest request) {
        // Validate order exists
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // TODO: Implement actual payment processing with external payment gateway
        // This is a stub implementation for MVP
        
        String transactionId = UUID.randomUUID().toString();
        log.info("Processing payment for order: {} with transaction: {}", orderId, transactionId);

        // Simulate payment processing
        boolean success = true; // In real implementation, call payment gateway

        return PaymentResult.builder()
                .success(success)
                .transactionId(transactionId)
                .message(success ? "Payment processed successfully" : "Payment failed")
                .orderId(orderId)
                .build();
    }
}
