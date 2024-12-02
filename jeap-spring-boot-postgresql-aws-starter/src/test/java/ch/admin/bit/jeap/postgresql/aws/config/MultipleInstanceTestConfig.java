package ch.admin.bit.jeap.postgresql.aws.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * This class initializes the read-only (replica) database with recognizable database objects to be used in tests
 */
@Profile("replica")
@Configuration
public class MultipleInstanceTestConfig {

    @Bean
    public ReadOnlyDataSourceInitializer readOnlyDataSourceInitializer(@Qualifier("replicaDataSource") DataSource replicaDataSource) {
        return new ReadOnlyDataSourceInitializer(replicaDataSource);
    }

    @RequiredArgsConstructor
    public static class ReadOnlyDataSourceInitializer implements InitializingBean {
        private final DataSource dataSource;

        @Override
        public void afterPropertiesSet() {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.update("CREATE TABLE person (ID INT NOT NULL, FIRST_NAME VARCHAR(30), LAST_NAME VARCHAR(30))");
            jdbcTemplate.update("INSERT INTO person(ID, FIRST_NAME, LAST_NAME) VALUES (42, 'HansReadOnly', 'MusterReadOnly')");
        }
    }

}
