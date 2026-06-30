package org.example.idempotentodemesiparismotoru.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.idempotentodemesiparismotoru.dto.PaymentRequest;
import org.example.idempotentodemesiparismotoru.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Card-Number", defaultValue = "1234-5678-9012-3456") String cardNumber) {
        
        log.info("API İsteği Alındı. Sipariş ID: {}, Idempotency Key: {}", request.getOrderId(), request.getIdempotencyKey());
        
        // Servisi çağır (AOP ve Circuit Breaker burada devreye girecek)
        String responseMessage = paymentService.pay(request, cardNumber);
        
        return ResponseEntity.ok(responseMessage);
    }
}
