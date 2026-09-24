package ch.admin.bit.jeap.db.tx.config;

import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;

/** Observes completion without changing Spring's transaction or rollback semantics. */
final class AwsJdbcFailoverRetryTransactionListener implements TransactionExecutionListener {

    private static final ThreadLocal<Attempt> CURRENT_ATTEMPT = new ThreadLocal<>();

    static Attempt beginAttempt() {
        Attempt attempt = new Attempt();
        CURRENT_ATTEMPT.set(attempt);
        return attempt;
    }

    @Override
    public void beforeBegin(TransactionExecution transaction) {
        Attempt attempt = CURRENT_ATTEMPT.get();
        if (attempt != null && attempt.transaction == null) {
            attempt.transaction = transaction;
        }
    }

    @Override
    public void afterBegin(TransactionExecution transaction, Throwable failure) {
        Attempt attempt = CURRENT_ATTEMPT.get();
        if (attempt != null && attempt.transaction == transaction && failure != null) {
            attempt.safeToRetry = true; // Acquisition failed before the business method could run.
        }
    }

    @Override
    public void beforeCommit(TransactionExecution transaction) {
        Attempt attempt = CURRENT_ATTEMPT.get();
        if (attempt != null) {
            // Also exclude nested independent commits and commit failures with an uncertain outcome.
            attempt.commitAttempted = true;
        }
    }

    @Override
    public void afterRollback(TransactionExecution transaction, Throwable failure) {
        Attempt attempt = CURRENT_ATTEMPT.get();
        if (attempt != null && attempt.transaction == transaction) {
            attempt.safeToRetry = failure == null;
        }
    }

    static final class Attempt implements AutoCloseable {
        private TransactionExecution transaction;
        private boolean safeToRetry;
        private boolean commitAttempted;

        boolean canRetry() {
            // No callback means completion could not be observed: do not assume that rollback happened.
            return safeToRetry && !commitAttempted;
        }

        @Override
        public void close() {
            CURRENT_ATTEMPT.remove();
        }
    }
}
