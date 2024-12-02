package ch.admin.bit.jeap.db.tx;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.function.Supplier;

import static ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReadReplicaAwareTransactionManagerTest {

    PlatformTransactionManager platformTransactionManager = Mockito.mock(PlatformTransactionManager.class);
    Supplier<MeterRegistry> meterRegistrySupplier = SimpleMeterRegistry::new;
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