package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.RetryOnAwsJdbcFailover;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

class AwsJdbcFailoverRetryAdvisorTest {

    private final AnnotationTransactionAttributeSource transactionAttributeSource =
            new AnnotationTransactionAttributeSource();

    private AwsJdbcFailoverRetryAdvisor advisor;

    @BeforeEach
    void setUp() {
        advisor = advisor(false);
    }

    @Test
    void validateTransactionAttribute_acceptsRequiredPropagation() {
        TransactionAttribute transactionAttribute =
                new DefaultTransactionAttribute(Propagation.REQUIRED.value());

        assertThatCode(() -> AwsJdbcFailoverRetryAdvisor.validateTransactionAttribute(transactionAttribute))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = Propagation.class, mode = EnumSource.Mode.EXCLUDE, names = "REQUIRED")
    void validateTransactionAttribute_rejectsPropagationThatCouldEscapeAttemptTransaction(Propagation propagation) {
        TransactionAttribute transactionAttribute = new DefaultTransactionAttribute(propagation.value());

        assertThatIllegalStateException()
                .isThrownBy(() -> AwsJdbcFailoverRetryAdvisor.validateTransactionAttribute(transactionAttribute))
                .withMessage("@RetryOnAwsJdbcFailover requires @Transactional propagation REQUIRED");
    }

    @Test
    void pointcut_whenGlobalRetryIsDisabled_matchesOnlyExplicitlyAnnotatedMethods() throws Exception {
        assertThat(matches(advisor, DefaultService.class)).isFalse();
        assertThat(matches(advisor, RetryService.class)).isTrue();
    }

    @Test
    void pointcut_whenGlobalRetryIsEnabled_matchesRequiredTransactionalMethods() throws Exception {
        AwsJdbcFailoverRetryAdvisor globalAdvisor = advisor(true);

        assertThat(matches(globalAdvisor, DefaultService.class)).isTrue();
        assertThat(matches(globalAdvisor, RequiresNewService.class)).isFalse();
    }

    private AwsJdbcFailoverRetryAdvisor advisor(boolean globalRetryEnabled) {
        return new AwsJdbcFailoverRetryAdvisor(transactionAttributeSource,
                new AwsJdbcFailoverRetryProperties(globalRetryEnabled, 2, 0));
    }

    private static boolean matches(AwsJdbcFailoverRetryAdvisor advisor, Class<?> serviceClass)
            throws NoSuchMethodException {
        Method method = serviceClass.getDeclaredMethod("operation");
        return advisor.getPointcut().getMethodMatcher().matches(method, serviceClass);
    }

    private static class DefaultService {
        @Transactional
        public void operation() {
            // Intentionally empty: only the method's transaction metadata is relevant to this test.
        }
    }

    private static class RetryService {
        @RetryOnAwsJdbcFailover
        @Transactional
        public void operation() {
            // Intentionally empty: only the method's transaction metadata is relevant to this test.
        }
    }

    private static class RequiresNewService {
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void operation() {
            // Intentionally empty: only the method's transaction metadata is relevant to this test.
        }
    }
}
