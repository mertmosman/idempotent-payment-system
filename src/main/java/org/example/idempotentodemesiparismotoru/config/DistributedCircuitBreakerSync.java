package org.example.idempotentodemesiparismotoru.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DistributedCircuitBreakerSync implements MessageListener {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    private static final String TOPIC_NAME = "circuit-breaker-state-topic";
    private static final String SENDER_PREFIX = "STATE_CHANGE:";

    @PostConstruct
    public void init() {
        // 1. Subscribe to Redis Topic
        redisMessageListenerContainer.addMessageListener(this, new ChannelTopic(TOPIC_NAME));

        // 2. Listen to local Circuit Breaker state changes and publish to Redis
        CircuitBreaker bankApiCb = circuitBreakerRegistry.circuitBreaker("bankApi");
        bankApiCb.getEventPublisher().onStateTransition(event -> {
            String newState = event.getStateTransition().getToState().name();
            log.info("Yerel Circuit Breaker state değişti: {}. Redis'e yayınlanıyor...", newState);
            
            // Publish message format: STATE_CHANGE:OPEN
            redisTemplate.convertAndSend(TOPIC_NAME, SENDER_PREFIX + newState);
        });
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String msg = new String(message.getBody(), StandardCharsets.UTF_8);
        if (msg.startsWith(SENDER_PREFIX)) {
            String stateStr = msg.substring(SENDER_PREFIX.length());
            log.info("Redis'ten state değişim sinyali alındı: {}", stateStr);

            CircuitBreaker bankApiCb = circuitBreakerRegistry.circuitBreaker("bankApi");
            String currentState = bankApiCb.getState().name();

            if (!currentState.equals(stateStr)) {
                log.info("Yerel Circuit Breaker durumu Redis'e göre güncelleniyor: {} -> {}", currentState, stateStr);
                switch (stateStr) {
                    case "OPEN":
                        bankApiCb.transitionToOpenState();
                        break;
                    case "CLOSED":
                        bankApiCb.transitionToClosedState();
                        break;
                    case "HALF_OPEN":
                        bankApiCb.transitionToHalfOpenState();
                        break;
                    default:
                        log.warn("Bilinmeyen state: {}", stateStr);
                }
            }
        }
    }
}
