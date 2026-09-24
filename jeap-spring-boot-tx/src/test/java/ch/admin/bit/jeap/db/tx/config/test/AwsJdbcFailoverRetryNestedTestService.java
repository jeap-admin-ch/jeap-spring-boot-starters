package ch.admin.bit.jeap.db.tx.config.test;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AwsJdbcFailoverRetryNestedTestService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void insertPerson() {
        jdbcTemplate.update("INSERT INTO person(ID, FIRST_NAME, LAST_NAME) VALUES (3, 'Global', 'Retry')");
    }
}
