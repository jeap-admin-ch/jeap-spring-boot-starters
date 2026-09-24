package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.RetryOnAwsJdbcFailover;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

import static ch.admin.bit.jeap.db.tx.AwsJdbcFailoverExceptionClassifier.isRetryable;

@Slf4j
public class AwsJdbcFailoverRetryAdvisor implements PointcutAdvisor, MethodInterceptor, Ordered {

    private static final ThreadLocal<Boolean> RETRY_ACTIVE = new ThreadLocal<>();

    private final TransactionAttributeSource transactionAttributeSource;
    private final AwsJdbcFailoverRetryProperties properties;
    private final Pointcut pointcut = new RetryPointcut();

    public AwsJdbcFailoverRetryAdvisor(TransactionAttributeSource transactionAttributeSource,
                                       AwsJdbcFailoverRetryProperties properties) {
        this.transactionAttributeSource = transactionAttributeSource;
        this.properties = properties;
        validate(new RetrySettings(properties.maxAttempts(), properties.backoffMillis()));
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (Boolean.TRUE.equals(RETRY_ACTIVE.get())) {
            return invocation.proceed();
        }

        Class<?> targetClass = targetClass(invocation);
        RetryOnAwsJdbcFailover retryAnnotation = retryAnnotation(invocation.getMethod(), targetClass);
        TransactionAttribute transactionAttribute =
                transactionAttributeSource.getTransactionAttribute(invocation.getMethod(), targetClass);
        if (transactionAttribute == null) {
            throw new IllegalStateException("@RetryOnAwsJdbcFailover requires a transactional method");
        }
        if (retryAnnotation != null) {
            validateTransactionAttribute(transactionAttribute);
        } else if (transactionAttribute.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRED) {
            return invocation.proceed();
        }

        // A REQUIRED invocation inside an existing transaction must keep participating in that transaction.
        // Retrying it independently would commit part of the caller's unit of work and break atomicity.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return invocation.proceed();
        }

        RetrySettings retrySettings = retryAnnotation == null
                ? new RetrySettings(properties.maxAttempts(), properties.backoffMillis())
                : new RetrySettings(retryAnnotation.maxAttempts(), retryAnnotation.backoffMillis());
        validate(retrySettings);

        RETRY_ACTIVE.set(true);
        try {
            return retry(invocation, targetClass, retrySettings);
        } finally {
            RETRY_ACTIVE.remove();
        }
    }

    private Object retry(MethodInvocation invocation,
                         Class<?> targetClass,
                         RetrySettings retrySettings) throws Throwable {
        int attempt = 1;
        while (true) {
            try {
                // Re-run the remaining advice chain, including Spring's ordinary transaction interceptor. It creates,
                // commits and rolls back exactly one transaction per attempt with the originally selected manager.
                return retryableInvocation(invocation).proceed();
            } catch (Throwable throwable) {
                if (!isRetryable(throwable) || attempt >= retrySettings.maxAttempts()) {
                    throw throwable;
                }
                log.info("AWS JDBC connection failover interrupted {}. " +
                         "Retrying with a new transaction (attempt {}/{})",
                        methodName(invocation.getMethod(), targetClass), attempt + 1, retrySettings.maxAttempts());
                try {
                    backoff(retrySettings.backoffMillis());
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    throwable.addSuppressed(interruption);
                    throw throwable;
                }
                attempt++;
            }
        }
    }

    private static RetryOnAwsJdbcFailover retryAnnotation(Method method, Class<?> targetClass) {
        RetryOnAwsJdbcFailover retry = AnnotatedElementUtils.findMergedAnnotation(
                AopUtils.getMostSpecificMethod(method, targetClass), RetryOnAwsJdbcFailover.class);
        if (retry == null) {
            retry = AnnotatedElementUtils.findMergedAnnotation(method, RetryOnAwsJdbcFailover.class);
        }
        return retry;
    }

    private static Class<?> targetClass(MethodInvocation invocation) {
        Object target = invocation.getThis();
        return target == null ? invocation.getMethod().getDeclaringClass() : AopUtils.getTargetClass(target);
    }

    static void validateTransactionAttribute(TransactionAttribute transactionAttribute) {
        if (transactionAttribute.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRED) {
            throw new IllegalStateException("@RetryOnAwsJdbcFailover requires @Transactional propagation REQUIRED");
        }
    }

    private static MethodInvocation retryableInvocation(MethodInvocation invocation) {
        if (invocation instanceof ProxyMethodInvocation proxyMethodInvocation) {
            return proxyMethodInvocation.invocableClone();
        }
        throw new IllegalStateException("AWS JDBC failover retries require a Spring proxy method invocation");
    }

    private static String methodName(Method method, Class<?> targetClass) {
        return ClassUtils.getQualifiedMethodName(method, targetClass);
    }

    private static void validate(RetrySettings retrySettings) {
        if (retrySettings.maxAttempts() < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (retrySettings.backoffMillis() < 0) {
            throw new IllegalArgumentException("backoffMillis must not be negative");
        }
    }

    private static void backoff(long backoffMillis) throws InterruptedException {
        if (backoffMillis > 0) {
            Thread.sleep(backoffMillis);
        }
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this;
    }

    @Override
    public boolean isPerInstance() {
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private final class RetryPointcut extends StaticMethodMatcherPointcut {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            if (retryAnnotation(method, targetClass) != null) {
                return true;
            }
            if (!properties.enabled()) {
                return false;
            }
            TransactionAttribute transactionAttribute =
                    transactionAttributeSource.getTransactionAttribute(method, targetClass);
            return transactionAttribute != null &&
                   transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED;
        }
    }

    private record RetrySettings(int maxAttempts, long backoffMillis) {
    }
}
