package ch.admin.bit.jeap.db.tx;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Compatibility alias for {@link RetryOnDatabaseFailover}.
 *
 * @deprecated Use the platform-independent {@link RetryOnDatabaseFailover} instead.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated(since = "25.18.0", forRemoval = true)
@RetryOnDatabaseFailover
public @interface RetryOnAwsJdbcFailover {

    @AliasFor(annotation = RetryOnDatabaseFailover.class, attribute = "maxAttempts")
    int maxAttempts() default 2;

    @AliasFor(annotation = RetryOnDatabaseFailover.class, attribute = "backoffMillis")
    long backoffMillis() default 100;
}
