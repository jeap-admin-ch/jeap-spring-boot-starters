package ch.admin.bit.jeap.starter.application;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = DbPoolingDefaultsEnvPostProcessorWithOverrideTest.TestApp.class,
        properties = {
                "spring.application.name=test-app",
                "spring.datasource.hikari.minimum-idle=2",
                "spring.datasource.hikari.maximum-pool-size=10",
                "spring.datasource.hikari.keepalive-time=60000",
                "spring.datasource.hikari.pool-name=test-pool"})
class DbPoolingDefaultsEnvPostProcessorWithOverrideTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void postProcessEnvironment() {
        assertTrue(dataSource instanceof HikariDataSource);
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertEquals(2, hikariDataSource.getMinimumIdle());
        assertEquals(10, hikariDataSource.getMaximumPoolSize());
        assertEquals(60000, hikariDataSource.getKeepaliveTime());
        assertEquals("test-pool", hikariDataSource.getPoolName());
    }

    @SpringBootApplication
    static class TestApp {
    }
}