package ch.admin.bit.jeap.db.tx;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReadReplicaAwareTransactionManagerTest {

    private static final String JEAP_AWS_DB_TRANSACTION_READREPLICA = "jeap_db_transaction_readreplica";
    private static final String JEAP_AWS_DB_TRANSACTION_RW = "jeap_db_transaction_rw";

    PlatformTransactionManager platformTransactionManager = Mockito.mock(PlatformTransactionManager.class);
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    Supplier<MeterRegistry> meterRegistrySupplier = () -> meterRegistry;
    ReadReplicaAwareTransactionManager readReplicaAwareTransactionManager = new ReadReplicaAwareTransactionManager(platformTransactionManager, true, meterRegistrySupplier);

    @BeforeEach
    void setUp() {
        NESTING_LEVEL.remove();
        TOP_LEVEL_TRANSACTION_READ_ONLY.remove();
        TOP_LEVEL_TRANSACTION_ROUTED_TO_READ_REPLICA.remove();
        when(platformTransactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        doNothing().when(platformTransactionManager).commit(any());
        doNothing().when(platformTransactionManager).rollback(any());
    }

    @Test
    void getTransaction_onTopLevelTransaction_nestingLevelIsUpdated() {
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());

        assertEquals(1, NESTING_LEVEL.get().get());
        assertThreadLocalsAreSet();
    }

    @Test
    void getTransaction_onTopLevelTransaction_whenCommitting_nestingLevelIsUpdated() {
        TransactionStatus transactionStatus = readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        readReplicaAwareTransactionManager.commit(transactionStatus);

        assertEquals(0, NESTING_LEVEL.get().get());
        assertThreadLocalsAreEmpty();
    }

    @Test
    void getTransaction_onTopLevelTransaction_whenRollbacking_nestingLevelIsUpdated() {
        TransactionStatus transactionStatus = readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        readReplicaAwareTransactionManager.rollback(transactionStatus);

        assertEquals(0, NESTING_LEVEL.get().get());
        assertThreadLocalsAreEmpty();
    }

    @Test
    void getTransaction_onNestedTransaction_nestingLevelIsUpdated() {
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());

        assertEquals(2, NESTING_LEVEL.get().get());
        assertThreadLocalsAreSet();
    }

    @Test
    void getTransaction_onNestedTransaction_whenCommitting_nestingLevelIsUpdated() {
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        TransactionStatus transactionStatus = readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        readReplicaAwareTransactionManager.commit(transactionStatus);

        assertEquals(1, NESTING_LEVEL.get().get());
        assertThreadLocalsAreSet();
    }

    @Test
    void getTransaction_onNestedTransaction_whenRollbacking_nestingLevelIsUpdated() {
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        TransactionStatus transactionStatus = readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        readReplicaAwareTransactionManager.rollback(transactionStatus);

        assertEquals(1, NESTING_LEVEL.get().get());
        assertThreadLocalsAreSet();
    }

    @Test
    void getTransaction_onTopLevelTransaction_whenOpeningTransactionFails_nestingLevelShouldBeReset() {
        when(platformTransactionManager.getTransaction(any())).thenThrow(RuntimeException.class);

        try {
            readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        } catch (Exception e) {
            //Expected, we simulate an error when opening the transaction, this has happened for instance when
            //a connection timeout is thrown when opening the physical connection to the database
        }
        assertEquals(0, NESTING_LEVEL.get().get());
        assertThreadLocalsAreEmpty();
    }

    @Test
    void getTransaction_onNestedTransaction_whenOpeningTransactionFails_nestingLevelShouldBeReset() {
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        assertEquals(1, NESTING_LEVEL.get().get());
        assertThreadLocalsAreSet();

        when(platformTransactionManager.getTransaction(any())).thenThrow(RuntimeException.class);
        try {
            readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        } catch (Exception e) {
            //Expected, we simulate an error when opening the transaction, this has happened for instance when
            //a connection timeout is thrown when opening the physical connection to the database
        }
        assertEquals(1, NESTING_LEVEL.get().get());
        assertThreadLocalsAreSet();
    }

    @Test
    void getTransaction_onTopLevelTransaction_whenRoutedToReadReplica_readReplicaTransactionIsCounted() {
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());

        assertEquals(1, counterCount(JEAP_AWS_DB_TRANSACTION_READREPLICA));
        assertEquals(0, counterCount(JEAP_AWS_DB_TRANSACTION_RW));
    }

    @Test
    void getTransaction_onTopLevelTransaction_whenNotRoutedToReadReplica_readWriteTransactionIsCounted() {
        ReadReplicaAwareTransactionManager transactionManager =
                new ReadReplicaAwareTransactionManager(platformTransactionManager, false, meterRegistrySupplier);

        transactionManager.getTransaction(new DefaultTransactionDefinition());

        assertEquals(1, counterCount(JEAP_AWS_DB_TRANSACTION_RW));
        assertEquals(0, counterCount(JEAP_AWS_DB_TRANSACTION_READREPLICA));
    }

    @Test
    void getTransaction_onNestedTransaction_isNotCounted() {
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());
        readReplicaAwareTransactionManager.getTransaction(getReadOnlyTransactionDefinition());

        assertEquals(1, counterCount(JEAP_AWS_DB_TRANSACTION_READREPLICA));
    }

    @Test
    void getTransaction_whenMeterRegistryIsNotAvailable_transactionIsStillStarted() {
        ReadReplicaAwareTransactionManager transactionManager = new ReadReplicaAwareTransactionManager(
                platformTransactionManager, true, () -> {
            throw new IllegalStateException("Meter registry not available yet");
        });

        assertNotNull(transactionManager.getTransaction(getReadOnlyTransactionDefinition()));
        assertEquals(1, NESTING_LEVEL.get().get());
    }

    @Test
    void getTransaction_whenTransactionsAreStartedConcurrently_allTransactionsAreCountedWithoutFailing() throws Exception {
        // The counters are created lazily on the first transaction. Transactions started concurrently by other
        // threads at that moment must not see a partially initialized state, as observed in production where the
        // first messages consumed from kafka failed with a NullPointerException on the read-write counter.
        ReadReplicaAwareTransactionManager transactionManager =
                new ReadReplicaAwareTransactionManager(platformTransactionManager, false, meterRegistrySupplier);
        int threadCount = 32;
        CyclicBarrier startBarrier = new CyclicBarrier(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    startBarrier.await(10, TimeUnit.SECONDS);
                    TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                    transactionManager.commit(status);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(30, TimeUnit.SECONDS), "Threads did not finish in time");
        assertEquals(List.of(), failures, "Starting transactions concurrently must not fail");
        assertEquals(threadCount, counterCount(JEAP_AWS_DB_TRANSACTION_RW));
    }

    @Test
    void getTransaction_whileCountersAreBeingCreated_concurrentTransactionIsNotAffected() throws Exception {
        // Reproduces the production failure: while one thread was creating the counters, another thread starting a
        // transaction observed a partially initialized state and failed with a NullPointerException on the
        // read-write counter. The counters must therefore become visible to other threads all at once.
        CountDownLatch counterCreationStarted = new CountDownLatch(1);
        Supplier<MeterRegistry> slowMeterRegistrySupplier = new Supplier<>() {
            private final AtomicInteger invocations = new AtomicInteger();

            @Override
            public MeterRegistry get() {
                if (invocations.incrementAndGet() == 1) {
                    counterCreationStarted.countDown();
                } else {
                    // Resolving the registry more than once means the counters are created one after the other:
                    // stay in here long enough for the other thread to observe the incomplete state.
                    sleep(500);
                }
                return meterRegistry;
            }
        };
        ReadReplicaAwareTransactionManager transactionManager =
                new ReadReplicaAwareTransactionManager(platformTransactionManager, false, slowMeterRegistrySupplier);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        Thread counterCreatingThread = startThread(failures, () ->
                transactionManager.getTransaction(new DefaultTransactionDefinition()));
        Thread concurrentThread = startThread(failures, () -> {
            assertTrue(counterCreationStarted.await(10, TimeUnit.SECONDS), "Counter creation did not start in time");
            sleep(100);
            transactionManager.getTransaction(new DefaultTransactionDefinition());
        });
        counterCreatingThread.join(TimeUnit.SECONDS.toMillis(30));
        concurrentThread.join(TimeUnit.SECONDS.toMillis(30));

        assertEquals(List.of(), failures, "Starting a transaction while the counters are created must not fail");
        assertEquals(2, counterCount(JEAP_AWS_DB_TRANSACTION_RW));
    }

    private static Thread startThread(List<Throwable> failures, ThrowingRunnable runnable) {
        return Thread.ofVirtual().start(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                failures.add(t);
            }
        });
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private double counterCount(String counterName) {
        return meterRegistry.find(counterName).counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private static DefaultTransactionDefinition getReadOnlyTransactionDefinition() {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setReadOnly(true);
        return transactionDefinition;
    }

    private void assertThreadLocalsAreEmpty() {
        assertNull(TOP_LEVEL_TRANSACTION_READ_ONLY.get());
        assertNull(TOP_LEVEL_TRANSACTION_ROUTED_TO_READ_REPLICA.get());
    }

    private void assertThreadLocalsAreSet() {
        assertNotNull(TOP_LEVEL_TRANSACTION_READ_ONLY.get());
        assertNotNull(TOP_LEVEL_TRANSACTION_ROUTED_TO_READ_REPLICA.get());
    }
}