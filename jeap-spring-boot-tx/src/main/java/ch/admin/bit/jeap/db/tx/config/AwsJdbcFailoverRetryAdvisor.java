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
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

import static ch.admin.bit.jeap.db.tx.AwsJdbcFailoverExceptionClassifier.isRetryable;

@Slf4j
public class AwsJdbcFailoverRetryAdvisor extends TransactionAspectSupport
        implements PointcutAdvisor, MethodInterceptor, Ordered {

    private static final ThreadLocal<Boolean> RETRY_ACTIVE = new ThreadLocal<>();

    private final TransactionAttributeSource transactionAttributeSource;
    private final AwsJdbcFailoverRetryProperties properties;
    private final Pointcut pointcut = new RetryPointcut();

    public AwsJdbcFailoverRetryAdvisor(ListableBeanFactory beanFactory,
                                       TransactionAttributeSource transactionAttributeSource,
                                       TransactionManager defaultTransactionManager,
                                       AwsJdbcFailoverRetryProperties properties) {
        this.transactionAttributeSource = transactionAttributeSource;
        this.properties = properties;
        validate(new RetrySettings(properties.maxAttempts(), properties.backoffMillis()));
        setBeanFactory(beanFactory);
        setTransactionAttributeSource(transactionAttributeSource);
        if (defaultTransactionManager != null) {
            setTransactionManager(defaultTransactionManager);
        }
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

        RetrySettings retrySettings = retryAnnotation == null
                ? new RetrySettings(properties.maxAttempts(), properties.backoffMillis())
                : new RetrySettings(retryAnnotation.maxAttempts(), retryAnnotation.backoffMillis());
        validate(retrySettings);
        PlatformTransactionManager transactionManager = transactionManager(transactionAttribute, targetClass);

        RETRY_ACTIVE.set(true);
        try {
            return retry(invocation, targetClass, transactionManager, transactionAttribute, retrySettings);
        } finally {
            RETRY_ACTIVE.remove();
        }
    }

    private Object retry(MethodInvocation invocation,
                         Class<?> targetClass,
                         PlatformTransactionManager transactionManager,
                         TransactionAttribute transactionAttribute,
                         RetrySettings retrySettings) throws Throwable {
        int attempt = 1;
        while (true) {
            try {
                return executeInNewTransaction(invocation, targetClass, transactionManager, transactionAttribute);
            } catch (Throwable throwable) {
                if (!isRetryable(throwable) || attempt >= retrySettings.maxAttempts()) {
                    throw throwable;
                }
                log.info("AWS JDBC connection failover interrupted {}. " +
                         "Retrying with a new transaction (attempt {}/{})",
                        methodName(invocation.getMethod(), targetClass), attempt + 1, retrySettings.maxAttempts());
                backoff(retrySettings.backoffMillis());
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

    private static Object executeInNewTransaction(MethodInvocation invocation,
                                                  Class<?> targetClass,
                                                  PlatformTransactionManager transactionManager,
                                                  TransactionAttribute transactionAttribute) throws Throwable {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute(transactionAttribute);
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        definition.setName(methodName(invocation.getMethod(), targetClass));

        TransactionStatus status = transactionManager.getTransaction(definition);
        Object result;
        try {
            result = retryableInvocation(invocation).proceed();
        } catch (Throwable throwable) {
            completeAfterException(transactionManager, transactionAttribute, status, throwable);
            throw throwable;
        }
        transactionManager.commit(status);
        return result;
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
            try {
                Thread.sleep(backoffMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            }
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
