package ch.admin.bit.jeap.db.tx;

import java.lang.annotation.*;

/**
 * Opts an idempotent operation into retries after a supported database failover,
 * independently of whether global failover retries are enabled.
 * The annotation is platform-independent; retryable failures are determined by the implementation.
 * Currently, only AWS Advanced JDBC Wrapper FailoverSuccessSQLException with SQL state 08S02 is supported.
 * A retry is performed only when the annotated invocation starts its own transaction. If it participates in an
 * existing transaction, normal {@code REQUIRED} semantics are preserved and no retry is attempted. This annotation
 * must be combined with {@code @Transactional} using its default {@code REQUIRED} propagation and only used for
 * operations that are safe to repeat.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryOnDatabaseFailover {

    int maxAttempts() default 2;

    long backoffMillis() default 100;
}
