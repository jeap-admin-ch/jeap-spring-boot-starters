package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.RetryOnAwsJdbcFailover;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

class AwsJdbcFailoverRetryAdvisorTest {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    private final AnnotationTransactionAttributeSource transactionAttributeSource =
            new AnnotationTransactionAttributeSource();
    private final PlatformTransactionManager configuredDefault = new StubTransactionManager();
    private final PlatformTransactionManager secondary = new StubTransactionManager();

    private AwsJdbcFailoverRetryAdvisor advisor;

    @BeforeEach
    void setUp() {
        beanFactory.registerSingleton("configuredDefaultTransactionManager", configuredDefault);
        beanFactory.registerSingleton("secondaryTransactionManager", secondary);
        advisor = advisor(false);
    }

    @Test
    void transactionManager_usesConfiguredDefaultWhenMultipleManagersExist() throws Exception {
        TransactionAttribute transactionAttribute = transactionAttribute(DefaultService.class);

        PlatformTransactionManager result = advisor.transactionManager(transactionAttribute, DefaultService.class);

        assertThat(result).isSameAs(configuredDefault);
    }

    @Test
    void transactionManager_honorsTransactionAttributeQualifier() throws Exception {
        TransactionAttribute transactionAttribute = transactionAttribute(ExplicitlyQualifiedService.class);

        PlatformTransactionManager result =
                advisor.transactionManager(transactionAttribute, ExplicitlyQualifiedService.class);

        assertThat(result).isSameAs(secondary);
    }

    @Test
    void transactionManager_honorsClassLevelQualifier() throws Exception {
        TransactionAttribute transactionAttribute = transactionAttribute(ClassQualifiedService.class);

        PlatformTransactionManager result = advisor.transactionManager(transactionAttribute, ClassQualifiedService.class);

        assertThat(result).isSameAs(secondary);
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
        return new AwsJdbcFailoverRetryAdvisor(beanFactory, transactionAttributeSource, configuredDefault,
                new AwsJdbcFailoverRetryProperties(globalRetryEnabled, 2, 0));
    }

    private static boolean matches(AwsJdbcFailoverRetryAdvisor advisor, Class<?> serviceClass)
            throws NoSuchMethodException {
        Method method = serviceClass.getDeclaredMethod("operation");
        return advisor.getPointcut().getMethodMatcher().matches(method, serviceClass);
    }

    private TransactionAttribute transactionAttribute(Class<?> serviceClass) throws NoSuchMethodException {
        Method method = serviceClass.getDeclaredMethod("operation");
        TransactionAttribute transactionAttribute =
                transactionAttributeSource.getTransactionAttribute(method, serviceClass);
        assertThat(transactionAttribute).isNotNull();
        return transactionAttribute;
    }

    private static class DefaultService {
        @Transactional
        public void operation() {
            // Intentionally empty: only the method's transaction metadata is relevant to this test.
        }
    }

    private static class ExplicitlyQualifiedService {
        @Transactional("secondaryTransactionManager")
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

    @Qualifier("secondaryTransactionManager")
    private static class ClassQualifiedService {
        @Transactional
        public void operation() {
            // Intentionally empty: only the method's transaction metadata is relevant to this test.
        }
    }

    private static class StubTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            // Intentionally empty: this stub only supports transaction-manager resolution tests.
        }

        @Override
        public void rollback(TransactionStatus status) {
            // Intentionally empty: this stub only supports transaction-manager resolution tests.
        }
    }
}
