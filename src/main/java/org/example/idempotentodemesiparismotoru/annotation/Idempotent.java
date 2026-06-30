package org.example.idempotentodemesiparismotoru.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    
    /**
     * SpEL expression to evaluate the idempotency key dynamically from method arguments.
     * Example: "#paymentRequest.idempotencyKey"
     */
    String keyExpression() default "";
    
    /**
     * Duration for how long the idempotency key should be locked in Redis.
     */
    long timeout() default 10;
    
    /**
     * Time unit for the timeout.
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
