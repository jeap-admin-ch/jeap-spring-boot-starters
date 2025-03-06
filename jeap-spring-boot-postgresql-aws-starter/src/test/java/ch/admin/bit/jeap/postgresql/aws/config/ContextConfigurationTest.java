package ch.admin.bit.jeap.postgresql.aws.config;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionRoutingDataSource;
import ch.admin.bit.jeap.postgresql.aws.TestConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContextConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void contextConfiguredExplicitSpringJdbcDataSourceIsIgnored() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("jeap.postgresql.aws.enabled=true")
                .withPropertyValues("jeap.datasource.aws.enable-advanced-jdbc-wrapper=false")
                .withPropertyValues("jeap.datasource.url=jdbc:h2:mem:readwrite;DB_CLOSE_ON_EXIT=FALSE")
                .withPropertyValues("spring.datasource.url=jdbc:h2:mem:dummy")
                .withPropertyValues("spring.datasource.username=user")
                .withPropertyValues("spring.datasource.password=pass")
                .withPropertyValues("jeap.datasource.hikari.schema=PUBLIC")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();

                            assertThat(context).hasBean("dataSource");
                            assertThat(context.getBean("dataSource")).isInstanceOf(HikariDataSource.class);
                            HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                            assertThat(dataSource.getJdbcUrl()).doesNotContain("jdbc:h2:mem:dummy");

                            assertThat(context).hasBean("transactionRoutingDataSource");
                            assertThat(context.getBean("transactionRoutingDataSource")).isInstanceOf(ReadReplicaAwareTransactionRoutingDataSource.class);

                            assertThat(context).doesNotHaveBean("replicaDataSource");

                            assertThat(context).hasBean("transactionManager");
                            assertThat(context.getBean("transactionManager")).isInstanceOf(ReadReplicaAwareTransactionManager.class);
                        }
                );

    }

    @Test
    void contextConfiguredExplicitJeapSingleDataSource() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("jeap.postgresql.aws.enabled=true")
                .withPropertyValues("jeap.datasource.aws.enable-advanced-jdbc-wrapper=false")
                .withPropertyValues("jeap.datasource.url=jdbc:h2:mem:dummy")
                .withPropertyValues("jeap.datasource.username=user")
                .withPropertyValues("jeap.datasource.password=pass")
                .withPropertyValues("jeap.datasource.hikari.schema=PUBLIC")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();

                            assertThat(context).hasBean("dataSource");
                            assertThat(context.getBean("dataSource")).isInstanceOf(HikariDataSource.class);
                            HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                            assertThat(dataSource.getJdbcUrl()).contains("jdbc:h2:mem:dummy");

                            assertThat(context).hasBean("transactionRoutingDataSource");
                            assertThat(context.getBean("transactionRoutingDataSource")).isInstanceOf(ReadReplicaAwareTransactionRoutingDataSource.class);

                            assertThat(context).doesNotHaveBean("replicaDataSource");

                            assertThat(context).hasBean("transactionManager");
                            assertThat(context.getBean("transactionManager")).isInstanceOf(ReadReplicaAwareTransactionManager.class);
                        }
                );
    }

    @Test
    void contextConfiguredExplicitJeapMultipleDataSourceDisabledByDefault() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("jeap.postgresql.aws.enabled=true")
                .withPropertyValues("jeap.datasource.aws.enable-advanced-jdbc-wrapper=false")
                .withPropertyValues("jeap.datasource.url=jdbc:h2:mem:dummy")
                .withPropertyValues("jeap.datasource.username=user")
                .withPropertyValues("jeap.datasource.password=pass")
                .withPropertyValues("jeap.datasource.hikari.schema=PUBLIC")
                .withPropertyValues("jeap.datasource.replica.url=jdbc:h2:mem:dummy-readonly")
                .withPropertyValues("jeap.datasource.replica.username=user")
                .withPropertyValues("jeap.datasource.replica.password=pass")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();

                            assertThat(context).hasBean("dataSource");
                            assertThat(context.getBean("dataSource")).isInstanceOf(HikariDataSource.class);
                            HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                            assertThat(dataSource.getJdbcUrl()).contains("jdbc:h2:mem:dummy");

                            assertThat(context).hasBean("transactionRoutingDataSource");
                            assertThat(context.getBean("transactionRoutingDataSource")).isInstanceOf(ReadReplicaAwareTransactionRoutingDataSource.class);

                            assertThat(context).doesNotHaveBean("replicaDataSource");

                            assertThat(context).hasBean("transactionManager");
                            assertThat(context.getBean("transactionManager")).isInstanceOf(ReadReplicaAwareTransactionManager.class);
                        }
                );
    }

    @Test
    void contextConfiguredExplicitJeapMultipleDataSource() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("jeap.postgresql.aws.enabled=true")
                .withPropertyValues("jeap.datasource.url=jdbc:h2:mem:dummy")
                .withPropertyValues("jeap.datasource.aws.enable-advanced-jdbc-wrapper=false")
                .withPropertyValues("jeap.datasource.username=user")
                .withPropertyValues("jeap.datasource.password=pass")
                .withPropertyValues("jeap.datasource.hikari.schema=PUBLIC") // Default H2 schema
                .withPropertyValues("jeap.datasource.replica.enabled=true")
                .withPropertyValues("jeap.datasource.replica.url=jdbc:h2:mem:dummy-readonly")
                .withPropertyValues("jeap.datasource.replica.username=user")
                .withPropertyValues("jeap.datasource.replica.password=pass")
                .withPropertyValues("jeap.datasource.replica.hikari.schema=PUBLIC") // Default H2 schema
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();

                            assertThat(context).hasBean("dataSource");
                            assertThat(context.getBean("dataSource")).isInstanceOf(HikariDataSource.class);
                            HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                            assertThat(dataSource.getJdbcUrl()).contains("jdbc:h2:mem:dummy");

                            assertThat(context).hasBean("transactionRoutingDataSource");
                            assertThat(context.getBean("transactionRoutingDataSource")).isInstanceOf(ReadReplicaAwareTransactionRoutingDataSource.class);

                            assertThat(context).hasBean("replicaDataSource");
                            HikariDataSource replicaDataSource = (HikariDataSource) context.getBean("replicaDataSource");
                            assertThat(replicaDataSource.getJdbcUrl()).contains("jdbc:h2:mem:dummy-readonly");

                            assertThat(context).hasBean("transactionManager");
                            assertThat(context.getBean("transactionManager")).isInstanceOf(ReadReplicaAwareTransactionManager.class);
                        }
                );
    }

    @Test
    void contextConfiguredExplicitJeapMultipleDataSource_inferredUrl_dbNameInferred() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("spring.jpa.hibernate.ddl-auto=none")
                .withPropertyValues("spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
                .withPropertyValues("spring.application.name=theapp")
                .withPropertyValues("jeap.postgresql.aws.enabled=true")
                .withPropertyValues("jeap.datasource.aws.enable-advanced-jdbc-wrapper=false")
                .withPropertyValues("jeap.datasource.driverClassname=org.h2.Driver")
                .withPropertyValues("jeap.datasource.aws.hostname=somehost")
                .withPropertyValues("jeap.datasource.replica.aws.hostname=somereplicahost")
                .withPropertyValues("jeap.datasource.replica.enabled=true")
                .withPropertyValues("jeap.datasource.replica.driverClassname=org.h2.Driver")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(context).hasBean("dataSource");
                    assertThat(context.getBean("dataSource")).isInstanceOf(HikariDataSource.class);
                    HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                    assertThat(dataSource.getJdbcUrl()).contains("jdbc:postgresql://somehost:5432/theapp_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");

                    assertThat(context).hasBean("replicaDataSource");
                    HikariDataSource replicaDataSource = (HikariDataSource) context.getBean("replicaDataSource");
                    assertThat(replicaDataSource.getJdbcUrl()).contains("jdbc:postgresql://somereplicahost:5432/theapp_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
                });
    }

    @Test
    void contextConfiguredExplicitJeapMultipleDataSource_inferredUrl_dbNameSet() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("spring.jpa.hibernate.ddl-auto=none")
                .withPropertyValues("spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
                .withPropertyValues("jeap.postgresql.aws.enabled=true")
                .withPropertyValues("jeap.datasource.aws.enable-advanced-jdbc-wrapper=false")
                .withPropertyValues("jeap.datasource.driverClassname=org.h2.Driver")
                .withPropertyValues("jeap.datasource.aws.database-name=test_db")
                .withPropertyValues("jeap.datasource.aws.hostname=somehost")
                .withPropertyValues("jeap.datasource.replica.aws.hostname=somereplicahost")
                .withPropertyValues("jeap.datasource.replica.enabled=true")
                .withPropertyValues("jeap.datasource.replica.driverClassname=org.h2.Driver")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(context).hasBean("dataSource");
                    assertThat(context.getBean("dataSource")).isInstanceOf(HikariDataSource.class);
                    HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                    assertThat(dataSource.getJdbcUrl()).contains("jdbc:postgresql://somehost:5432/test_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");

                    assertThat(context).hasBean("replicaDataSource");
                    HikariDataSource replicaDataSource = (HikariDataSource) context.getBean("replicaDataSource");
                    assertThat(replicaDataSource.getJdbcUrl()).contains("jdbc:postgresql://somereplicahost:5432/test_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
                });
    }

    @Test
    void contextConfiguredExplicitJeapMultipleDataSource_inferredUrl_replicaDbNameSet() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("spring.jpa.hibernate.ddl-auto=none")
                .withPropertyValues("spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
                .withPropertyValues("jeap.postgresql.aws.enabled=true")
                .withPropertyValues("jeap.datasource.aws.enable-advanced-jdbc-wrapper=false")
                .withPropertyValues("jeap.datasource.driverClassname=org.h2.Driver")
                .withPropertyValues("jeap.datasource.aws.database-name=test_db")
                .withPropertyValues("jeap.datasource.aws.hostname=somehost")
                .withPropertyValues("jeap.datasource.replica.enabled=true")
                .withPropertyValues("jeap.datasource.replica.aws.hostname=somereplicahost")
                .withPropertyValues("jeap.datasource.replica.aws.database-name=replica_db")
                .withPropertyValues("jeap.datasource.replica.driverClassname=org.h2.Driver")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(context).hasBean("dataSource");
                    assertThat(context.getBean("dataSource")).isInstanceOf(HikariDataSource.class);
                    HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                    assertThat(dataSource.getJdbcUrl()).contains("jdbc:postgresql://somehost:5432/test_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");

                    assertThat(context).hasBean("replicaDataSource");
                    HikariDataSource replicaDataSource = (HikariDataSource) context.getBean("replicaDataSource");
                    assertThat(replicaDataSource.getJdbcUrl()).contains("jdbc:postgresql://somereplicahost:5432/replica_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
                });
    }

    @Test
    void contextConfiguredOnlyTransactionConfiguration() {
        contextRunner.withUserConfiguration(TestConfig.class)
                .withPropertyValues("jeap.postgresql.aws.enabled=false")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();

                            assertThat(context).hasBean("dataSource");
                            HikariDataSource dataSource = (HikariDataSource) context.getBean("dataSource");
                            assertThat(dataSource).isInstanceOf(HikariDataSource.class);
                            assertThat(dataSource.getJdbcUrl()).contains("jdbc:h2:mem:");

                            assertThat(context).hasBean("transactionManager");
                            assertThat(context.getBean("transactionManager")).isInstanceOf(ReadReplicaAwareTransactionManager.class);
                        }
                );
    }

}
