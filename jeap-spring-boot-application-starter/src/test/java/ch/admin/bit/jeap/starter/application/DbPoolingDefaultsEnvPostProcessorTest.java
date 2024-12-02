package ch.admin.bit.jeap.starter.application;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = DbPoolingDefaultsEnvPostProcessorTest.TestApp.class, properties = "spring.application.name=test-app")
class DbPoolingDefaultsEnvPostProcessorTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void postProcessEnvironment() {
        assertTrue(dataSource instanceof HikariDataSource);
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertEquals(0, hikariDataSource.getMinimumIdle());
        assertEquals(4, hikariDataSource.getMaximumPoolSize());
        assertEquals(120000, hikariDataSource.getKeepaliveTime());
        assertEquals("hikari-cp-test-app", hikariDataSource.getPoolName());
    }

    @SpringBootApplication
    static class TestApp {
    }
}