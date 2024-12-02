package ch.admin.bit.jeap.db.tx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * This DataSource implementation resolves a target DataSource depending on the read-only value of the top-level
 * transaction.
 * <p>
 * The method determineCurrentLookupKey is only called when the top-level transaction is started in a thread.
 */
@Slf4j
public class ReadReplicaAwareTransactionRoutingDataSource extends AbstractRoutingDataSource {

    public static final String READER_KEY = "reader";
    public static final String WRITER_KEY = "writer";

    @Override
    protected Object determineCurrentLookupKey() {
        boolean routeToReadReplica = ReadReplicaAwareTransactionManager.routeTopLevelTransactionToReadReplica();

        String resolvedLookupKey = routeToReadReplica ? READER_KEY : WRITER_KEY;
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved datasource key: " + resolvedLookupKey);
        }
        return resolvedLookupKey;
    }
}
