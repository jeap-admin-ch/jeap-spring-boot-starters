package ch.admin.bit.jeap.db.tx;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This transaction manager keeps track of the readOnly value of top level transactions and sets this value to a
 * ThreadLocal, which can be used somewhere else, for instance by {@link ReadReplicaAwareTransactionRoutingDataSource} to route to the
 * corresponding datasource.
 * <p>
 * This manager acts as a wrapper of a PlatformTransactionManager, delegating all transaction handling operations to it.
 * <p>
 * In addition to that, it checks the readOnly value of nested transactions and forbids creating a non-readonly transaction
 * inside a top-level read-only one. This check preemptively avoids usage of nested transaction definitions not
 * suitable for AWS RDS with "reader" endpoints.
 * <p>
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/rds-proxy-endpoints.html#rds-proxy-endpoints-reader">AWS documentation</a>
 */
@Slf4j
public class ReadReplicaAwareTransactionManager implements PlatformTransactionManager {

    private static final String JEAP_AWS_DB_TRANSACTION_READREPLICA = "jeap_db_transaction_readreplica";
    private static final String JEAP_AWS_DB_TRANSACTION_RW = "jeap_db_transaction_rw";

    static final ThreadLocal<Boolean> TOP_LEVEL_TRANSACTION_READ_ONLY = new ThreadLocal<>();
    static final ThreadLocal<Boolean> TOP_LEVEL_TRANSACTION_ROUTED_TO_READ_REPLICA = new ThreadLocal<>();
    static final ThreadLocal<AtomicInteger> NESTING_LEVEL = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    private final PlatformTransactionManager delegate;

    @Getter
    private final boolean routeTransactionsToReadReplica;
    private final Supplier<MeterRegistry> meterRegistrySupplier;

    /**
     * The counters are created lazily, as the {@link MeterRegistry} cannot be resolved yet when this transaction
     * manager is created early in the spring context lifecycle. Both counters are held in a single immutable,
     * volatile field such that they are always published together: transactions may be started concurrently by
     * multiple threads as soon as the application accepts work, and a partially initialized state would lead to
     * NullPointerExceptions failing those transactions.
     */
    private volatile TransactionCounters transactionCounters;
    private final AtomicBoolean counterInitializationFailureLogged = new AtomicBoolean();

    public ReadReplicaAwareTransactionManager(PlatformTransactionManager delegate,
                                              boolean routeTransactionsToReadReplica,
                                              Supplier<MeterRegistry> meterRegistrySupplier) {
        this.delegate = delegate;
        this.routeTransactionsToReadReplica = routeTransactionsToReadReplica;
        this.meterRegistrySupplier = meterRegistrySupplier;
    }

    private TransactionCounters getOrCreateCounters() {
        TransactionCounters existingCounters = transactionCounters;
        if (existingCounters != null) {
            return existingCounters;
        }
        MeterRegistry meterRegistry = meterRegistrySupplier.get();
        // Concurrent initialization is harmless: the meter registry returns the already registered meter for a
        // meter id that has been registered before, i.e. all threads end up using the same counter instances.
        TransactionCounters createdCounters = new TransactionCounters(
                Counter.builder(JEAP_AWS_DB_TRANSACTION_READREPLICA)
                        .description("Transactions routed to read replicas")
                        .register(meterRegistry),
                Counter.builder(JEAP_AWS_DB_TRANSACTION_RW)
                        .description("Writer instance transactions")
                        .register(meterRegistry));
        transactionCounters = createdCounters;
        return createdCounters;
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        if (log.isDebugEnabled()) {
            log.debug("Transaction definition is " + (definition.isReadOnly() ? "read-only" : "read-write"));
        }
        if (isTopLevelTransaction()) {
            if (routeTransactionsToReadReplica && !definition.isReadOnly()) {
                throw new IllegalStateException("Read-write transactions cannot be annotated with " +
                                                TransactionalReadReplica.class.getSimpleName() + " or handled by the " +
                                                getClass().getSimpleName() + " when routing to read replicas.");
            }

            setTopLevelTransactionReadOnly(definition.isReadOnly());
            setTopLevelTransactionRoutedToReadReplica(routeTransactionsToReadReplica);
            updateMetric(routeTransactionsToReadReplica);
        } else {
            /*
                Nesting read-write transaction definitions inside a top-level read-only transaction might fail in two ways:
                - The Hibernate flush mode will be set to NEVER, JPA writes might be lost silently
                - When Read-Only RDS replicas are used, they will refuse to execute writes
                Thus, this is validated here preemptively to fail early, for example in unit/integration tests.
             */
            if (!definition.isReadOnly() && isTopLevelTransactionReadOnly()) {
                throw new IllegalStateException(
                        "Read-write transactions cannot be nested in top level read-only transactions. " +
                        "This will lead to missing write flushes and read replicas refusing to execute writes.");
            }
        }

        NESTING_LEVEL.get().incrementAndGet();
        try {
            return delegate.getTransaction(definition);
        } catch (Exception e) {
            // If an exception happens while getting the transaction (i.e. when facing timeouts connecting to db),
            // we still need to reflect this in the ThreadLocals
            NESTING_LEVEL.get().decrementAndGet();
            if (isTopLevelTransaction()) {
                clearTransactionThreadLocals();
            }
            throw e;
        }
    }

    private void updateMetric(boolean readReplica) {
        TransactionCounters counters;
        try {
            counters = getOrCreateCounters();
        } catch (RuntimeException e) {
            // Counting transactions must never make transaction handling fail. Resolving the meter registry can
            // fail while the spring context is still starting up, in which case the counters are created on a
            // later transaction.
            if (counterInitializationFailureLogged.compareAndSet(false, true)) {
                log.debug("Unable to create the transaction counters, transactions will not be counted until the " +
                          "meter registry is available.", e);
            }
            return;
        }
        if (readReplica) {
            counters.readReplica().increment();
        } else {
            counters.readWrite().increment();
        }
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        if (log.isDebugEnabled()) {
            log.debug("Committing transaction.");
        }
        try {
            delegate.commit(status);
        } finally {
            NESTING_LEVEL.get().decrementAndGet();
            if (isTopLevelTransaction()) {
                clearTransactionThreadLocals();
            }
        }
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        if (log.isDebugEnabled()) {
            log.debug("Rolling back transaction.");
        }
        try {
            delegate.rollback(status);
        } finally {
            NESTING_LEVEL.get().decrementAndGet();
            if (isTopLevelTransaction()) {
                clearTransactionThreadLocals();
            }
        }
    }

    private static boolean isTopLevelTransaction() {
        return NESTING_LEVEL.get().get() == 0;
    }

    private static void setTopLevelTransactionRoutedToReadReplica(boolean routeTransactionsToReadReplica) {
        TOP_LEVEL_TRANSACTION_ROUTED_TO_READ_REPLICA.set(routeTransactionsToReadReplica);
    }

    private static void setTopLevelTransactionReadOnly(boolean isReadOnly) {
        TOP_LEVEL_TRANSACTION_READ_ONLY.set(isReadOnly);
    }

    public static boolean routeTopLevelTransactionToReadReplica() {
        Boolean value = TOP_LEVEL_TRANSACTION_ROUTED_TO_READ_REPLICA.get();
        return value != null && value;
    }

    private static Boolean isTopLevelTransactionReadOnly() {
        return TOP_LEVEL_TRANSACTION_READ_ONLY.get();
    }

    private static void clearTransactionThreadLocals() {
        TOP_LEVEL_TRANSACTION_READ_ONLY.remove();
        TOP_LEVEL_TRANSACTION_ROUTED_TO_READ_REPLICA.remove();
    }

    private record TransactionCounters(Counter readReplica, Counter readWrite) {
    }
}
