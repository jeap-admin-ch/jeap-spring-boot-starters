package ch.admin.bit.jeap.db.tx;

import java.lang.annotation.*;

/**
 * Opts an idempotent operation into retries after the AWS Advanced JDBC Wrapper has successfully replaced a failed
 * connection, independently of whether global failover retries are enabled.
 * A retry is performed only when the annotated invocation starts its own transaction. If it participates in an
 * existing transaction, normal {@code REQUIRED} semantics are preserved and no retry is attempted. This annotation
 * must be combined with {@code @Transactional} using its default {@code REQUIRED} propagation and only used for
 * operations that are safe to repeat.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryOnAwsJdbcFailover {

    int maxAttempts() default 2;

    long backoffMillis() default 100;
}
