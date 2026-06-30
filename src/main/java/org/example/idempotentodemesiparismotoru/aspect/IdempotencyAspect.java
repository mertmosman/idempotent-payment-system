package org.example.idempotentodemesiparismotoru.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.idempotentodemesiparismotoru.annotation.Idempotent;
import org.example.idempotentodemesiparismotoru.exception.IdempotencyException;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    private static final String IDEMPOTENCY_PREFIX = "idempotency:lock:";

    @Around("@annotation(idempotent)")
    public Object processIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = generateKey(joinPoint, idempotent.keyExpression());
        String lockKey = IDEMPOTENCY_PREFIX + key;

        boolean isLocked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(
                        lockKey,
                        "locked",
                        Duration.of(idempotent.timeout(), idempotent.timeUnit().toChronoUnit())
                )
        );

        if (!isLocked) {
            log.warn("Duplicate request detected for idempotency key: {}", key);
            throw new IdempotencyException("Bu işlem daha önce gerçekleştirilmiştir veya şu an işleniyor. Lütfen bekleyiniz.");
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // In case of an actual system failure (not a business failure), we could potentially release the lock.
            // However, in standard idempotency, if a payment fails for business logic (e.g. insufficient funds),
            // keeping the lock might be desired so they don't retry and fail again.
            // For now, we will NOT release the lock automatically on exception to maintain strict idempotency.
            // redisTemplate.delete(lockKey); 
            throw e;
        }
    }

    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (keyExpression == null || keyExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency keyExpression cannot be empty.");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = discoverer.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        String evaluatedKey = parser.parseExpression(keyExpression).getValue(context, String.class);
        if (evaluatedKey == null) {
            throw new IllegalArgumentException("Evaluated idempotency key is null for expression: " + keyExpression);
        }

        return evaluatedKey;
    }
}
