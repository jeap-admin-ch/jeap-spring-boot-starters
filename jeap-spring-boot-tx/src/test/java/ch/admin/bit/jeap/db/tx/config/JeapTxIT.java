package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.db.tx.config.test.AwsJdbcFailoverRetryTestService;
import ch.admin.bit.jeap.db.tx.config.test.Person;
import ch.admin.bit.jeap.db.tx.config.test.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jeap.datasource.aws.failover-retry.enabled=true",
        "jeap.datasource.aws.failover-retry.backoff-millis=0"
})
class JeapTxIT {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AwsJdbcFailoverRetryTestService failoverRetryTestService;

    @Test
    void ensureTransactionalReadReplicaWorksWithoutSpecificDataSourceRoutingConfiguration() {
        List<Person> results = personRepository.findAll();
        Optional<Person> maybePerson = personRepository.findPersonById(1);

        assertThat(results).extracting(Person::getId).contains(1);
        assertThat(maybePerson).isPresent();
    }

    @Test
    @TransactionalReadReplica
    void ensureTransactionalReadReplicaWorksWithoutSpecificDataSourceRoutingConfiguration_nestedTransactions() {
        Optional<Person> maybePerson = personRepository.findPersonById(1);

        assertThat(maybePerson).isPresent();
    }

    @Test
    @Transactional(readOnly = true)
    void awsJdbcFailoverRetryStartsANewTransaction() {
        failoverRetryTestService.insertWithFailoverAfterFirstInsert();

        assertThat(failoverRetryTestService.attempts()).isEqualTo(2);
        assertThat(personRepository.findById(2)).isPresent();
    }

    @Test
    void globallyEnabledAwsJdbcFailoverRetryStartsANewTransactionAndDoesNotRetryNestedRepositoryCalls() {
        failoverRetryTestService.insertWithGloballyEnabledRetry();

        assertThat(failoverRetryTestService.globalAttempts()).isEqualTo(2);
        assertThat(personRepository.findById(3)).isPresent();
    }
}
