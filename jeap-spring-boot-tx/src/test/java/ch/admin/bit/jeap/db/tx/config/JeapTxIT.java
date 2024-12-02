package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.db.tx.config.test.Person;
import ch.admin.bit.jeap.db.tx.config.test.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class JeapTxIT {

    @Autowired
    private PersonRepository personRepository;

    @Test
    void ensureTransactionalReadReplicaWorksWithoutSpecificDataSourceRoutingConfiguration() {
        List<Person> results = personRepository.findAll();
        Optional<Person> maybePerson = personRepository.findPersonById(1);

        assertThat(results).hasSize(1);
        assertThat(maybePerson).isPresent();
    }

    @Test
    @TransactionalReadReplica
    void ensureTransactionalReadReplicaWorksWithoutSpecificDataSourceRoutingConfiguration_nestedTransactions() {
        Optional<Person> maybePerson = personRepository.findPersonById(1);

        assertThat(maybePerson).isPresent();
    }
}
