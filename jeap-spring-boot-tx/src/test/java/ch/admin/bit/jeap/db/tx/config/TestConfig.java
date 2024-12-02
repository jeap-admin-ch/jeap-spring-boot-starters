package ch.admin.bit.jeap.db.tx.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class TestConfig {

    @Bean
    public MeterRegistry registry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public DataInitializer dataInitializer(JdbcTemplate jdbcTemplate) {
        return new DataInitializer(jdbcTemplate);
    }

    @RequiredArgsConstructor
    public static class DataInitializer implements InitializingBean {
        private final JdbcTemplate jdbcTemplate;

        @Override
        public void afterPropertiesSet() {
            jdbcTemplate.update("INSERT INTO person(ID, FIRST_NAME, LAST_NAME) VALUES (1, 'Hans', 'Muster')");
        }
    }
}
