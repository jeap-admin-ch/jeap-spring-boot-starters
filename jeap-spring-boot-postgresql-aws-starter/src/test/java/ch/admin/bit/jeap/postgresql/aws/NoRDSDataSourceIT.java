package ch.admin.bit.jeap.postgresql.aws;


import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.postgresql.aws.infrastructure.persistence.Person;
import ch.admin.bit.jeap.postgresql.aws.infrastructure.persistence.PersonRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This Integration Test ensures the functionality of {@link ReadReplicaAwareTransactionManager} even on non-RDS
 * datasources, which will allow to catch wrong transaction hierarchies before deploying logic to AWS
 */
@DirtiesContext
@ActiveProfiles("no-rds")
@SpringBootTest
class NoRDSDataSourceIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    @Qualifier("readReplicaTransactionManager")
    private PlatformTransactionManager readReplicaTransactionManager;

    @Test
    void contextConfigured() {
        assertTrue(dataSource instanceof HikariDataSource);
        assertTrue(platformTransactionManager instanceof ReadReplicaAwareTransactionManager);
    }

    @Test
    @TransactionalReadReplica
    void readOnlyTransaction() {
        List<Person> results = personRepository.findAll();

        assertEquals(0, results.size());
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
        TransactionTemplate transactionTemplate = new TransactionTemplate(readReplicaTransactionManager);
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

}
