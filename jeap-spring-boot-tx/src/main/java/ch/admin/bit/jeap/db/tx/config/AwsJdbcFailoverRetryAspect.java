package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.RetryOnAwsJdbcFailover;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

import java.lang.reflect.Method;

import static ch.admin.bit.jeap.db.tx.AwsJdbcFailoverExceptionClassifier.isRetryable;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AwsJdbcFailoverRetryAspect extends TransactionAspectSupport {

    private final TransactionAttributeSource transactionAttributeSource;

    public AwsJdbcFailoverRetryAspect(ListableBeanFactory beanFactory,
                                      TransactionAttributeSource transactionAttributeSource,
                                      TransactionManager defaultTransactionManager) {
        this.transactionAttributeSource = transactionAttributeSource;
        setBeanFactory(beanFactory);
        setTransactionAttributeSource(transactionAttributeSource);
        if (defaultTransactionManager != null) {
            setTransactionManager(defaultTransactionManager);
        }
    }

    @Around("@annotation(ch.admin.bit.jeap.db.tx.RetryOnAwsJdbcFailover)")
    public Object retryOnAwsJdbcFailover(ProceedingJoinPoint joinPoint) throws Throwable {
        RetryOnAwsJdbcFailover retry = retryAnnotation(joinPoint);
        validate(retry);
        Class<?> targetClass = targetClass(joinPoint);
        TransactionAttribute transactionAttribute = transactionAttribute(joinPoint, targetClass);
        validateTransactionAttribute(transactionAttribute);
        PlatformTransactionManager transactionManager = transactionManager(transactionAttribute, targetClass);
        int attempt = 1;
        while (true) {
            try {
                return executeInNewTransaction(joinPoint, transactionManager, transactionAttribute);
            } catch (Throwable throwable) {
                if (!isRetryable(throwable) || attempt >= retry.maxAttempts()) {
                    throw throwable;
                }
                log.info("AWS JDBC connection failover interrupted {}. Retrying with a new transaction (attempt {}/{})",
                        joinPoint.getSignature().toShortString(), attempt + 1, retry.maxAttempts());
                backoff(retry.backoffMillis());
                attempt++;
            }
        }
    }

    private static RetryOnAwsJdbcFailover retryAnnotation(ProceedingJoinPoint joinPoint) {
        RetryOnAwsJdbcFailover retry =
                AnnotatedElementUtils.findMergedAnnotation(targetMethod(joinPoint), RetryOnAwsJdbcFailover.class);
        if (retry == null) {
            retry = AnnotatedElementUtils.findMergedAnnotation(invokedMethod(joinPoint), RetryOnAwsJdbcFailover.class);
        }
        if (retry == null) {
            throw new IllegalStateException("Retry annotation is missing from advised method");
        }
        return retry;
    }

    private TransactionAttribute transactionAttribute(ProceedingJoinPoint joinPoint, Class<?> targetClass) {
        TransactionAttribute transactionAttribute =
                transactionAttributeSource.getTransactionAttribute(invokedMethod(joinPoint), targetClass);
        if (transactionAttribute == null) {
            throw new IllegalStateException("@RetryOnAwsJdbcFailover requires a transactional method");
        }
        return transactionAttribute;
    }

    private static Method targetMethod(ProceedingJoinPoint joinPoint) {
        return AopUtils.getMostSpecificMethod(invokedMethod(joinPoint), targetClass(joinPoint));
    }

    private static Method invokedMethod(ProceedingJoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod();
    }

    private static Class<?> targetClass(ProceedingJoinPoint joinPoint) {
        return AopProxyUtils.ultimateTargetClass(joinPoint.getTarget());
    }

    PlatformTransactionManager transactionManager(TransactionAttribute transactionAttribute, Class<?> targetClass) {
        TransactionManager transactionManager = determineTransactionManager(transactionAttribute, targetClass);
        if (transactionManager instanceof PlatformTransactionManager platformTransactionManager) {
            return platformTransactionManager;
        }
        throw new IllegalStateException("Specified transaction manager is not a PlatformTransactionManager: " +
                                        transactionManager);
    }

    static void validateTransactionAttribute(TransactionAttribute transactionAttribute) {
        if (transactionAttribute.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRED) {
            throw new IllegalStateException("@RetryOnAwsJdbcFailover requires @Transactional propagation REQUIRED");
        }
    }

    private static Object executeInNewTransaction(ProceedingJoinPoint joinPoint,
                                                  PlatformTransactionManager transactionManager,
                                                  TransactionAttribute transactionAttribute) throws Throwable {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute(transactionAttribute);
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        definition.setName(joinPoint.getSignature().toLongString());

        TransactionStatus status = transactionManager.getTransaction(definition);
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            completeAfterException(transactionManager, transactionAttribute, status, throwable);
            throw throwable;
        }
        transactionManager.commit(status);
        return result;
    }

    private static void completeAfterException(PlatformTransactionManager transactionManager,
                                               TransactionAttribute transactionAttribute,
                                               TransactionStatus status,
                                               Throwable throwable) {
        if (isRetryable(throwable) || transactionAttribute.rollbackOn(throwable)) {
            transactionManager.rollback(status);
        } else {
            transactionManager.commit(status);
        }
    }

    private static void validate(RetryOnAwsJdbcFailover retry) {
        if (retry.maxAttempts() < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (retry.backoffMillis() < 0) {
            throw new IllegalArgumentException("backoffMillis must not be negative");
        }
    }

    private static void backoff(long backoffMillis) throws InterruptedException {
        if (backoffMillis > 0) {
            try {
                Thread.sleep(backoffMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            }
        }
    }
}
