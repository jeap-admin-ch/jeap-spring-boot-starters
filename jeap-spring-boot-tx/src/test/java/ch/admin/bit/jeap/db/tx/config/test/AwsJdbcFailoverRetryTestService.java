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
    private final AtomicInteger unknownOutcomeAttempts = new AtomicInteger();

    @RetryOnAwsJdbcFailover(maxAttempts = 2, backoffMillis = 0)
    @Transactional
    public void insertWithFailoverAfterFirstInsert() {
        // Synthetic failure: verifies rollback and replay, not an actual AWS in-flight failover (08007).
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
        jdbcTemplate.update("INSERT INTO person(ID, FIRST_NAME, LAST_NAME) VALUES (5, 'Local', 'Rollback')");
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

    @Transactional
    public void insertWithUnknownTransactionOutcome() {
        unknownOutcomeAttempts.incrementAndGet();
        jdbcTemplate.update("INSERT INTO person(ID, FIRST_NAME, LAST_NAME) VALUES (6, 'Unknown', 'Outcome')");
        throw new IllegalStateException("simulated in-flight failover", new TransactionStateUnknownSQLException());
    }

    public int unknownOutcomeAttempts() {
        return unknownOutcomeAttempts.get();
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

    private static final class TransactionStateUnknownSQLException extends SQLException {
        private TransactionStateUnknownSQLException() {
            super("transaction outcome unknown", "08007");
        }
    }
}
