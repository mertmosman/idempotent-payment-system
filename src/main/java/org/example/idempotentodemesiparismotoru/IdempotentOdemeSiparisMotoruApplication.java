package org.example.idempotentodemesiparismotoru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IdempotentOdemeSiparisMotoruApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdempotentOdemeSiparisMotoruApplication.class, args);
    }

}
