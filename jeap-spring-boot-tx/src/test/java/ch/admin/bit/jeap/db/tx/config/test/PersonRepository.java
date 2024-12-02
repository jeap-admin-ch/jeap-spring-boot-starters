package ch.admin.bit.jeap.db.tx.config.test;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {

    @TransactionalReadReplica
    Optional<Person> findPersonById(Integer personId);
}
