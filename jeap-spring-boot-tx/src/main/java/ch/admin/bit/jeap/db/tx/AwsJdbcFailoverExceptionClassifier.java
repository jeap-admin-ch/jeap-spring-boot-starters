package ch.admin.bit.jeap.db.tx;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class AwsJdbcFailoverExceptionClassifier {

    private static final String FAILOVER_SUCCESS_EXCEPTION = "FailoverSuccessSQLException";
    private static final String COMMUNICATION_LINK_CHANGED_SQL_STATE = "08S02";

    private AwsJdbcFailoverExceptionClassifier() {
    }

    public static boolean isRetryable(Throwable throwable) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && visited.add(current)) {
            if (current instanceof SQLException sqlException
                    && COMMUNICATION_LINK_CHANGED_SQL_STATE.equals(sqlException.getSQLState())
                    && FAILOVER_SUCCESS_EXCEPTION.equals(sqlException.getClass().getSimpleName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
