package ch.admin.bit.jeap.db.tx.config.test;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AwsJdbcFailoverRetryCallerTestService {

    private final AwsJdbcFailoverRetryTestService retryTestService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertThenRollbackOuterTransaction() {
        retryTestService.insertInExistingTransaction();
        throw new ExpectedRollbackException();
    }

    public static final class ExpectedRollbackException extends RuntimeException {
    }
}
