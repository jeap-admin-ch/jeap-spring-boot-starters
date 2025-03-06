package ch.admin.bit.jeap.postgresql.aws;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionRoutingDataSource;
import ch.admin.bit.jeap.postgresql.aws.infrastructure.persistence.Person;
import ch.admin.bit.jeap.postgresql.aws.infrastructure.persistence.PersonRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
@ActiveProfiles("single")
@SpringBootTest
class SingleInstanceRDSDataSourceIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Test
    void contextConfigured() {
        assertTrue(dataSource instanceof ReadReplicaAwareTransactionRoutingDataSource);
        assertTrue(platformTransactionManager instanceof ReadReplicaAwareTransactionManager);
        ReadReplicaAwareTransactionRoutingDataSource readReplicaAwareTransactionRoutingDataSource = (ReadReplicaAwareTransactionRoutingDataSource) dataSource;

        Map<Object, DataSource> resolvedDataSources = readReplicaAwareTransactionRoutingDataSource.getResolvedDataSources();

        HikariDataSource rdsDataSource = (HikariDataSource) resolvedDataSources.get(ReadReplicaAwareTransactionRoutingDataSource.WRITER_KEY);
        assertEquals(25, rdsDataSource.getHikariConfigMXBean().getMaximumPoolSize());
        assertEquals("hik-pool", rdsDataSource.getHikariConfigMXBean().getPoolName());
        assertEquals("pass-rw", rdsDataSource.getPassword());

        assertNull(resolvedDataSources.get(ReadReplicaAwareTransactionRoutingDataSource.READER_KEY));
    }

    @Test
    void readOnlyTransaction() {
        Optional<Person> randomPerson1 = personRepository.findPersonById(new SecureRandom().nextInt());

        List<Person> persons = personRepository.findAll();

        assertTrue(randomPerson1.isEmpty());
        assertTrue(persons.isEmpty());
    }

    @Test
    @Transactional
    void readWriteTransaction() {
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
    @Transactional(readOnly = true)
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
        TransactionTemplate readOnlyTransactionTemplate = new TransactionTemplate(platformTransactionManager);
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

}
