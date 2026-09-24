package ch.admin.bit.jeap.db.tx.config.test;

import ch.admin.bit.jeap.db.tx.RetryOnAwsJdbcFailover;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class AwsJdbcFailoverRetryTestService {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicInteger attempts = new AtomicInteger();

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

    private static final class FailoverSuccessSQLException extends SQLException {
        private FailoverSuccessSQLException() {
            super("connection changed", "08S02");
        }
    }
}
