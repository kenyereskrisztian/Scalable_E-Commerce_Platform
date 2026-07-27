package com.ecommerce.controller;

import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResult;
import com.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public ResponseEntity<PaymentResult> processPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResult result = paymentService.processPayment(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
