package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.db.tx.config.test.AwsJdbcFailoverRetryCallerTestService;
import ch.admin.bit.jeap.db.tx.config.test.AwsJdbcFailoverRetryTestService;
import ch.admin.bit.jeap.db.tx.config.test.Person;
import ch.admin.bit.jeap.db.tx.config.test.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "jeap.datasource.aws.failover-retry.enabled=true",
        "jeap.datasource.aws.failover-retry.backoff-millis=0",
        "jeap.datasource.replica.enabled=true"
})
class JeapTxIT {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AwsJdbcFailoverRetryTestService failoverRetryTestService;

    @Autowired
    private AwsJdbcFailoverRetryCallerTestService failoverRetryCallerTestService;

    @Test
    void checkedFailoverThatCommitsIsNotRetried() {
        assertThatThrownBy(failoverRetryTestService::insertThenThrowCheckedFailover)
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessage("connection changed");
        assertThat(failoverRetryTestService.checkedFailureAttempts()).isEqualTo(1);
        assertThat(personRepository.findById(7)).isPresent();
    }

    @Test
    void noRollbackForFailoverThatCommitsIsNotRetried() {
        assertThatThrownBy(failoverRetryTestService::insertThenThrowNoRollbackFailover)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("committed despite failure");
        assertThat(failoverRetryTestService.noRollbackAttempts()).isEqualTo(1);
        assertThat(personRepository.findById(8)).isPresent();
    }

    @Test
    void checkedFailoverWithExplicitRollbackIsRetried() throws Exception {
        failoverRetryTestService.insertThenThrowCheckedFailoverWithRollback();
        assertThat(failoverRetryTestService.explicitRollbackAttempts()).isEqualTo(2);
        assertThat(personRepository.findById(9)).isPresent();
    }

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

    @Test
    void retryAnnotatedRequiredMethod_participatesInExistingTransaction() {
        assertThatThrownBy(failoverRetryCallerTestService::insertThenRollbackOuterTransaction)
                .isInstanceOf(AwsJdbcFailoverRetryCallerTestService.ExpectedRollbackException.class);

        assertThat(personRepository.findById(4)).isEmpty();
    }

    @Test
    void globallyEnabledRetry_preservesNormalSetRollbackOnlyBehavior() {
        assertThatCode(failoverRetryTestService::markCurrentTransactionRollbackOnly)
                .doesNotThrowAnyException();
        assertThat(personRepository.findById(5)).isEmpty();
    }

    @Test
    void globallyEnabledRetry_doesNotReplayUnknownTransactionOutcome() {
        assertThatThrownBy(failoverRetryTestService::insertWithUnknownTransactionOutcome)
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("transaction outcome unknown");

        assertThat(failoverRetryTestService.unknownOutcomeAttempts()).isEqualTo(1);
        // The synthetic H2 transaction rolls back. A real 08007 cannot guarantee this outcome.
        assertThat(personRepository.findById(6)).isEmpty();
    }

    @Test
    void globallyEnabledRetry_preservesReadReplicaRouting() {
        assertThat(failoverRetryTestService.isRoutedToReadReplica()).isTrue();
    }
}
