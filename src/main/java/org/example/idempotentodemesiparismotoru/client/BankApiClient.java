package org.example.idempotentodemesiparismotoru.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

@Component
@Slf4j
public class BankApiClient {

    private final Random random = new Random();

    /**
     * Simulates a call to an external bank API.
     * It randomly throws exceptions to simulate network issues or bank downtime.
     */
    public boolean processPayment(String cardNumber, BigDecimal amount) {
        log.info("Banka API'sine istek atılıyor. Tutar: {}", amount);
        
        // Simulate network delay
        try {
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 50% failure rate for demonstration
        if (random.nextInt(100) < 50) {
            log.error("Banka API'si yanıt vermedi (Timeout/500 Error)!");
            throw new RuntimeException("Banka servisi şu an ulaşılamaz durumda!");
        }

        log.info("Banka ödemeyi onayladı.");
        return true;
    }
}
