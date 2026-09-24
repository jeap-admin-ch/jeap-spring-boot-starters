package ch.admin.bit.jeap.db.tx;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class AwsJdbcFailoverExceptionClassifierTest {

    @Test
    void recognizesFailoverSuccessInCauseChain() {
        RuntimeException exception = new RuntimeException("data access failed", new FailoverSuccessSQLException());

        assertThat(AwsJdbcFailoverExceptionClassifier.isRetryable(exception)).isTrue();
    }

    @Test
    void rejectsUnknownTransactionStateAndUnrelatedSqlExceptions() {
        assertThat(AwsJdbcFailoverExceptionClassifier.isRetryable(
                new RuntimeException(new TransactionStateUnknownSQLException()))).isFalse();
        assertThat(AwsJdbcFailoverExceptionClassifier.isRetryable(
                new RuntimeException(new SQLException("connection failed", "08S02")))).isFalse();
    }

    private static final class FailoverSuccessSQLException extends SQLException {
        private FailoverSuccessSQLException() {
            super("connection changed", "08S02");
        }
    }

    private static final class TransactionStateUnknownSQLException extends SQLException {
        private TransactionStateUnknownSQLException() {
            super("transaction state unknown", "08007");
        }
    }
}
