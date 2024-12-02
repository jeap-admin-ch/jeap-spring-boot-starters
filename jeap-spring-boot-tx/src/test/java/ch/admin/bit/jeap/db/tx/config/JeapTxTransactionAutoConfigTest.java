package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JeapTxTransactionAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void platformTransactionManagerBeanPostProcessor_replicaEnabled() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("jeap.datasource.replica.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed();
                    assertThat(context)
                            .hasBean("transactionManager");
                    PlatformTransactionManager transactionManager = (PlatformTransactionManager) context.getBean("transactionManager");
                    assertThat(transactionManager)
                            .isInstanceOf(ReadReplicaAwareTransactionManager.class);

                    assertThat(context)
                            .hasBean("readReplicaTransactionManager");
                    ReadReplicaAwareTransactionManager readReplicaTransactionManager = (ReadReplicaAwareTransactionManager) context.getBean("readReplicaTransactionManager");
                    assertThat(readReplicaTransactionManager.isRouteTransactionsToReadReplica())
                            .isTrue();

                    assertThat(transactionManager)
                            .describedAs("Transaction manager should be a separate transaction manager, routing to read replica data source")
                            .isNotSameAs(readReplicaTransactionManager);
                });
    }

    @Test
    void platformTransactionManagerBeanPostProcessor_replicaDisabled() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("jeap.datasource.replica.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed();
                    assertThat(context)
                            .hasBean("transactionManager");
                    PlatformTransactionManager transactionManager = (PlatformTransactionManager) context.getBean("transactionManager");
                    assertThat(transactionManager)
                            .isInstanceOf(ReadReplicaAwareTransactionManager.class);

                    assertThat(context)
                            .hasBean("readReplicaTransactionManager");
                    ReadReplicaAwareTransactionManager readReplicaTransactionManager = (ReadReplicaAwareTransactionManager) context.getBean("readReplicaTransactionManager");
                    assertThat(readReplicaTransactionManager.isRouteTransactionsToReadReplica())
                            .isFalse();

                    assertThat(transactionManager)
                            .describedAs("Transaction manager should be an alias of the primary transaction manager")
                            .isSameAs(readReplicaTransactionManager);
                });
    }
}
