package ch.admin.bit.jeap.db.tx.config.test;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import ch.admin.bit.jeap.db.tx.RetryOnAwsJdbcFailover;
import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class AwsJdbcFailoverRetryTestService {

    private final JdbcTemplate jdbcTemplate;
    private final AwsJdbcFailoverRetryNestedTestService nestedTestService;
    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger globalAttempts = new AtomicInteger();

    @RetryOnAwsJdbcFailover(maxAttempts = 2, backoffMillis = 0)
    @Transactional
    public void insertWithFailoverAfterFirstInsert() {
        int attempt = attempts.incrementAndGet();
        jdbcTemplate.update("INSERT INTO person(ID, FIRST_NAME, LAST_NAME) VALUES (2, 'Aurora', 'Retry')");
        if (attempt == 1) {
            throw new IllegalStateException("simulated persistence failure", new FailoverSuccessSQLException());
        }
    }

    public int attempts() {
        return attempts.get();
    }

    @Transactional
    public void insertWithGloballyEnabledRetry() {
        int attempt = globalAttempts.incrementAndGet();
        nestedTestService.insertPerson();
        if (attempt == 1) {
            throw new IllegalStateException("simulated persistence failure", new FailoverSuccessSQLException());
        }
    }

    public int globalAttempts() {
        return globalAttempts.get();
    }

    @RetryOnAwsJdbcFailover(maxAttempts = 2, backoffMillis = 0)
    @Transactional
    public void insertInExistingTransaction() {
        jdbcTemplate.update("INSERT INTO person(ID, FIRST_NAME, LAST_NAME) VALUES (4, 'Atomic', 'Rollback')");
    }

    @Transactional
    public void markCurrentTransactionRollbackOnly() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

    @TransactionalReadReplica
    public boolean isRoutedToReadReplica() {
        return ReadReplicaAwareTransactionManager.routeTopLevelTransactionToReadReplica();
    }

    private static final class FailoverSuccessSQLException extends SQLException {
        private FailoverSuccessSQLException() {
            super("connection changed", "08S02");
        }
    }
}
