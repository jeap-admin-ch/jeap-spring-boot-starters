package ch.admin.bit.jeap.postgresql.aws;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionRoutingDataSource;
import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.postgresql.aws.infrastructure.persistence.Person;
import ch.admin.bit.jeap.postgresql.aws.infrastructure.persistence.PersonRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
@ActiveProfiles("replica")
@SpringBootTest
class MultipleInstanceRDSDataSourceIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    @Qualifier("readReplicaTransactionManager")
    private PlatformTransactionManager readReplicaTransactionManager;

    @Test
    void contextConfigured() {
        assertTrue(dataSource instanceof ReadReplicaAwareTransactionRoutingDataSource);
        assertTrue(platformTransactionManager instanceof ReadReplicaAwareTransactionManager);
        ReadReplicaAwareTransactionRoutingDataSource readReplicaAwareTransactionRoutingDataSource = (ReadReplicaAwareTransactionRoutingDataSource) dataSource;

        Map<Object, DataSource> resolvedDataSources = readReplicaAwareTransactionRoutingDataSource.getResolvedDataSources();

        RDSDataSource writerDataSource = (RDSDataSource) resolvedDataSources.get(ReadReplicaAwareTransactionRoutingDataSource.WRITER_KEY);
        assertNotNull(readReplicaAwareTransactionRoutingDataSource.getResolvedDefaultDataSource());
        assertEquals(writerDataSource, readReplicaAwareTransactionRoutingDataSource.getResolvedDefaultDataSource());
        assertEquals("jdbc:h2:mem:readwrite;DB_CLOSE_ON_EXIT=FALSE", writerDataSource.getJdbcUrl());
        assertEquals("user", writerDataSource.getUsername());
        assertEquals(25, writerDataSource.getHikariConfigMXBean().getMaximumPoolSize());
        assertEquals("hik-pool", writerDataSource.getHikariConfigMXBean().getPoolName());
        assertTrue(writerDataSource.getPassword().contains("?DBUser=user&Action=connect"));

        RDSDataSource readerDataSource = (RDSDataSource) resolvedDataSources.get(ReadReplicaAwareTransactionRoutingDataSource.READER_KEY);
        assertEquals("jdbc:h2:mem:readonly;DB_CLOSE_ON_EXIT=FALSE", readerDataSource.getJdbcUrl());
        assertEquals("user-ro", readerDataSource.getUsername());
        assertEquals(35, readerDataSource.getHikariConfigMXBean().getMaximumPoolSize());
        assertEquals("hik-ro-pool", readerDataSource.getHikariConfigMXBean().getPoolName());
        assertTrue(readerDataSource.getPassword().contains("?DBUser=user-ro&Action=connect"));
    }

    @Test
    @Transactional
    void writeJPATransactionGoesThroughWriterInstance() {
        Person.PersonBuilder personBuilder = Person.builder()
                .id(1)
                .firstName("Hans")
                .lastName("Muster");
        personRepository.save(personBuilder.build());

        List<Person> results = personRepository.findAll();

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId());
        assertEquals("Hans", results.get(0).getFirstName());
        assertEquals("Muster", results.get(0).getLastName());
    }

    @Test
    @Transactional(readOnly = true)
    void readOnlyJPATransactionGoesThroughWriterInstanceIfNotExplicitlyRoutedToReadReplica() {
        List<Person> results = personRepository.findAll();

        // MultipleInstanceTestConfig adds a record for 'HansReadOnly' in the read replica, and not in the writer
        // Thus we assert here that the writer instance is used by checking that no records are returned
        assertEquals(0, results.size());
    }

    @Test
    void testMetrics() {
        int readReplicaMetricCount = readReplicateMetricValue();
        int rwMetricCount = rwMetricValue();

        personRepository.findPersonById(1);

        int readReplicaMetricCountAfterRead = readReplicateMetricValue();

        personRepository.save(Person.builder().id(2).firstName("Henriette").lastName("Muster").build());

        int rwMetricCountAfterWrite = rwMetricValue();

        assertThat(readReplicaMetricCountAfterRead)
                .isEqualTo(readReplicaMetricCount + 1);
        assertThat(rwMetricCountAfterWrite)
                .isEqualTo(rwMetricCount + 1);
    }

    private int rwMetricValue() {
        return (int) meterRegistry.counter("jeap_db_transaction_rw").count();
    }

    private int readReplicateMetricValue() {
        return (int) meterRegistry.counter("jeap_db_transaction_readreplica").count();
    }

    @Test
    @TransactionalReadReplica
    void readJPATransactionGoesThroughReaderInstance() {
        List<Person> results = personRepository.findAll();

        assertEquals(1, results.size());
        assertEquals(42, results.get(0).getId());
        assertEquals("HansReadOnly", results.get(0).getFirstName());
        assertEquals("MusterReadOnly", results.get(0).getLastName());
    }

    @Test
    @Transactional
    void writeJdbcTransactionGoesThroughWriterInstance() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("insert into person values (?, ?, ?)", 1, "Hans", "Muster");

        List<Map<String, Object>> results = jdbcTemplate.queryForList("select * from person");

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).get("ID"));
        assertEquals("Hans", results.get(0).get("FIRST_NAME"));
        assertEquals("Muster", results.get(0).get("LAST_NAME"));
    }

    @Test
    @TransactionalReadReplica
    void readJdbcTransactionGoesThroughReaderInstance() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
        List<Map<String, Object>> results = jdbcTemplate.queryForList("select * from person");

        assertEquals(1, results.size());
        assertEquals(42, results.get(0).get("ID"));
        assertEquals("HansReadOnly", results.get(0).get("FIRST_NAME"));
        assertEquals("MusterReadOnly", results.get(0).get("LAST_NAME"));
    }

    @Test
    @TransactionalReadReplica
    void writeOperationInReadOnlyTransactionShouldFail() {
        InvalidDataAccessApiUsageException thrown = assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            Person.PersonBuilder personBuilder = Person.builder()
                    .id(1)
                    .firstName("Hans")
                    .lastName("Muster");
            personRepository.save(personBuilder.build());
        });
        assertTrue(thrown.getMessage().contains("Read-write transactions cannot be nested in top level read-only transactions."));
    }

    @Test
    @TransactionalReadReplica
    void writeTransactionNestedInTopLevelReadTransactionShouldFail() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setReadOnly(false);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                Person.PersonBuilder personBuilder = Person.builder()
                        .id(1)
                        .firstName("Hans")
                        .lastName("Muster");
                personRepository.save(personBuilder.build());
            });
        });
        assertTrue(thrown.getMessage().contains("Read-write transactions cannot be nested in top level read-only transactions."));
    }

    @Test
    void writeTransactionNestedInTopLevelReadTransactionUsingTransactionTemplateShouldFail() {
        assertNotSame(readReplicaTransactionManager, platformTransactionManager);

        TransactionTemplate readOnlyTransactionTemplate = new TransactionTemplate(readReplicaTransactionManager);
        readOnlyTransactionTemplate.setReadOnly(true);

        readOnlyTransactionTemplate.executeWithoutResult(readOnlyTransactionStatus -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            transactionTemplate.setReadOnly(false);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                transactionTemplate.executeWithoutResult(transactionStatus -> {
                    Person.PersonBuilder personBuilder = Person.builder()
                            .id(1)
                            .firstName("Hans")
                            .lastName("Muster");
                    personRepository.save(personBuilder.build());
                });
            });
            assertTrue(thrown.getMessage().contains("Read-write transactions cannot be nested in top level read-only transactions."));
        });
    }

    @Test
    @Transactional
    void readTransactionNestedInWriteTransactionShouldUseWriterInstance() {
        Person.PersonBuilder personBuilder = Person.builder()
                .id(1)
                .firstName("Hans")
                .lastName("Muster");
        personRepository.save(personBuilder.build());

        TransactionTemplate readOnlyTransactionTemplate = new TransactionTemplate(platformTransactionManager);
        readOnlyTransactionTemplate.setReadOnly(true);
        readOnlyTransactionTemplate.executeWithoutResult(roTransactionStatus -> {
            List<Person> results = personRepository.findAll();

            assertEquals(1, results.size());
            assertEquals(1, results.get(0).getId());
            assertEquals("Hans", results.get(0).getFirstName());
            assertEquals("Muster", results.get(0).getLastName());
        });
    }

    @Test
    @Transactional
    void readTransactionNestedInWriteTransactionUsingTransactionTemplateShouldUseWriterInstance() {
        TransactionTemplate readWriteTransactionTemplate = new TransactionTemplate(platformTransactionManager);
        readWriteTransactionTemplate.setReadOnly(false);
        readWriteTransactionTemplate.executeWithoutResult(transactionStatus -> {
            Person.PersonBuilder personBuilder = Person.builder()
                    .id(1)
                    .firstName("Hans")
                    .lastName("Muster");
            personRepository.save(personBuilder.build());

            TransactionTemplate readOnlyTransactionTemplate = new TransactionTemplate(platformTransactionManager);
            readOnlyTransactionTemplate.setReadOnly(true);
            readOnlyTransactionTemplate.executeWithoutResult(roTransactionStatus -> {
                List<Person> results = personRepository.findAll();

                assertEquals(1, results.size());
                assertEquals(1, results.get(0).getId());
                assertEquals("Hans", results.get(0).getFirstName());
                assertEquals("Muster", results.get(0).getLastName());
            });
        });

    }

}
