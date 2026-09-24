package ch.admin.bit.jeap.db.tx.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AwsJdbcFailoverRetryExecutionTest {

    private final RecordingTransactionManager manager = new RecordingTransactionManager();
    private final RetryService target = new RetryService();

    @Test
    void exhaustionStopsAtConfiguredLimitAndPropagatesLastFailure() {
        List<RuntimeException> failures = new ArrayList<>();
        target.operation = () -> {
            RuntimeException failure = new IllegalStateException("attempt " + target.calls, new FailoverSuccessSQLException());
            failures.add(failure);
            throw failure;
        };
        RetryService service = proxy(3, 0);

        Throwable thrown = catchThrowable(service::execute);

        assertThat(target.calls).isEqualTo(3);
        assertThat(thrown).isSameAs(failures.getLast());
        assertThat(manager.started).hasSize(3).doesNotHaveDuplicates();
        assertThat(manager.rolledBack).containsExactlyElementsOf(manager.started);
        assertThat(manager.committed).isEmpty();

        // Exhaustion must release the retry guard, so a later invocation can retry again.
        catchThrowable(service::execute);
        assertThat(target.calls).isEqualTo(6);
    }

    @Test
    void retryableAcquisitionFailureRetriesBeforeCallingBusinessMethod() {
        manager.acquisitionFailures.add(acquisitionFailure());
        RetryService service = proxy(3, 0);

        service.execute();

        assertThat(manager.acquisitions).isEqualTo(2);
        assertThat(target.calls).isEqualTo(1);
        assertThat(manager.started).hasSize(1);
        assertThat(manager.committed).containsExactlyElementsOf(manager.started);
        assertThat(manager.rolledBack).isEmpty();
    }

    @Test
    void acquisitionFailuresExhaustAttemptsWithoutCallingBusinessMethod() {
        RuntimeException first = acquisitionFailure();
        RuntimeException last = acquisitionFailure();
        manager.acquisitionFailures.add(first);
        manager.acquisitionFailures.add(last);
        RetryService service = proxy(2, 0);

        assertThatThrownBy(service::execute).isSameAs(last);

        assertThat(manager.acquisitions).isEqualTo(2);
        assertThat(target.calls).isZero();
        assertThat(manager.started).isEmpty();
        assertThat(manager.committed).isEmpty();
        assertThat(manager.rolledBack).isEmpty();
    }

    @Test
    void unrelatedAcquisitionFailureIsNotRetried() {
        RuntimeException failure = new CannotCreateTransactionException("unavailable", new SQLException("connection", "08001"));
        manager.acquisitionFailures.add(failure);
        RetryService service = proxy(3, 0);

        assertThatThrownBy(service::execute).isSameAs(failure);

        assertThat(manager.acquisitions).isEqualTo(1);
        assertThat(target.calls).isZero();
        assertThat(manager.started).isEmpty();
    }

    @Test
    void interruptedBackoffPreservesDatabaseFailureAndInterruptFlag() {
        RuntimeException failure = new IllegalStateException("database failure", new FailoverSuccessSQLException());
        target.operation = () -> {
            Thread.currentThread().interrupt();
            throw failure;
        };
        RetryService service = proxy(3, 100);

        try {
            assertThatThrownBy(service::execute).isSameAs(failure);
            assertThat(failure.getSuppressed()).singleElement().isInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(target.calls).isEqualTo(1);
            assertThat(manager.rolledBack).hasSize(1);
            assertThat(manager.committed).isEmpty();
        } finally {
            Thread.interrupted(); // Clear only the test thread's interrupt flag after verifying its preservation.
        }
    }

    private RetryService proxy(int attempts, long backoffMillis) {
        AnnotationTransactionAttributeSource attributes = new AnnotationTransactionAttributeSource();
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(attributes);
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvisor(new AwsJdbcFailoverRetryAdvisor(attributes,
                new AwsJdbcFailoverRetryProperties(true, attempts, backoffMillis)));
        factory.addAdvice(interceptor);
        return (RetryService) factory.getProxy();
    }

    private static RuntimeException acquisitionFailure() {
        return new CannotCreateTransactionException("failover while acquiring transaction", new FailoverSuccessSQLException());
    }

    static class RetryService {
        private int calls;
        private Runnable operation = () -> {
            // Default successful operation for transaction-acquisition tests.
        };

        @Transactional
        public void execute() {
            calls++;
            operation.run();
        }
    }

    private static class RecordingTransactionManager implements PlatformTransactionManager {
        private int acquisitions;
        private final Deque<RuntimeException> acquisitionFailures = new ArrayDeque<>();
        private final List<TransactionStatus> started = new ArrayList<>();
        private final List<TransactionStatus> committed = new ArrayList<>();
        private final List<TransactionStatus> rolledBack = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            acquisitions++;
            if (!acquisitionFailures.isEmpty()) {
                throw acquisitionFailures.removeFirst();
            }
            TransactionStatus status = new SimpleTransactionStatus(true);
            started.add(status);
            return status;
        }

        @Override
        public void commit(TransactionStatus status) {
            committed.add(status);
        }

        @Override
        public void rollback(TransactionStatus status) {
            rolledBack.add(status);
        }
    }

    private static class FailoverSuccessSQLException extends SQLException {
        private FailoverSuccessSQLException() {
            super("connection changed", "08S02");
        }
    }
}
