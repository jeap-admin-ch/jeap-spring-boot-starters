package ch.admin.bit.jeap.db.tx;

import java.lang.annotation.*;

/**
 * Retries an idempotent operation after the AWS Advanced JDBC Wrapper has successfully replaced a failed connection.
 * Any existing transaction is suspended and each attempt runs in a new transaction. This annotation must be combined
 * with {@code @Transactional} using its default {@code REQUIRED} propagation and only used for operations that are
 * safe to repeat.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryOnAwsJdbcFailover {

    int maxAttempts() default 2;

    long backoffMillis() default 100;
}
