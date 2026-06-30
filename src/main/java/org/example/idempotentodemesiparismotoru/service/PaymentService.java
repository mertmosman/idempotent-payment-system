package org.example.idempotentodemesiparismotoru.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.idempotentodemesiparismotoru.annotation.Idempotent;
import org.example.idempotentodemesiparismotoru.client.BankApiClient;
import org.example.idempotentodemesiparismotoru.dto.PaymentRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BankApiClient bankApiClient;

    @Idempotent(keyExpression = "#request.idempotencyKey", timeout = 30)
    @CircuitBreaker(name = "bankApi", fallbackMethod = "paymentFallback")
    public String pay(PaymentRequest request, String cardNumber) {
        log.info("Ödeme işlemi başlatıldı. Sipariş ID: {}, Idempotency: {}", request.getOrderId(), request.getIdempotencyKey());
        
        // This call might fail due to Bank API issues
        boolean isSuccess = bankApiClient.processPayment(cardNumber, request.getAmount());
        
        if (isSuccess) {
            return "Ödeme Başarılı! Sipariş onaylandı.";
        } else {
            return "Ödeme Reddedildi (Banka kaynaklı).";
        }
    }

    /**
     * Fallback method must have the same signature as the original method, 
     * plus a Throwable parameter at the end to catch the exception that triggered the fallback.
     */
    public String paymentFallback(PaymentRequest request, String cardNumber, Throwable t) {
        log.error("Circuit Breaker Devrede! Banka sistemine ulaşılamıyor. Hata: {}", t.getMessage());
        return "Şu an ödeme sistemlerimizde geçici bir yoğunluk var. Siparişiniz 'Beklemede' (PENDING) statüsüne alındı. Lütfen daha sonra tekrar deneyin.";
    }
}
