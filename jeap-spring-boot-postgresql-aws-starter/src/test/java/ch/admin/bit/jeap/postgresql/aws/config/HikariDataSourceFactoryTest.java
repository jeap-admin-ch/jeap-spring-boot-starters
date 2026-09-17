package ch.admin.bit.jeap.postgresql.aws.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import software.amazon.jdbc.util.HikariCPSQLException;

import static org.junit.jupiter.api.Assertions.*;

class HikariDataSourceFactoryTest {

    @Test
    void createWithWrapper() {
        WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties = new WrapperTargetDataSourceProperties();
        wrapperTargetDataSourceProperties.put("wrapperPlugins", "plugin1");
        HikariDataSource hikariDataSource = HikariDataSourceFactory.create(new DataSourceProperties(), wrapperTargetDataSourceProperties,  "myUser", "jdbc:postgresql://localhost:5432/db");

        assertNotNull(hikariDataSource);
        assertEquals("jdbc:aws-wrapper:postgresql://localhost:5432/db", hikariDataSource.getDataSourceProperties().getProperty("jdbcUrl"));
        assertEquals("myUser", hikariDataSource.getUsername());
        assertEquals(HikariCPSQLException.class.getName(), hikariDataSource.getExceptionOverrideClassName());
        WrapperTargetDataSourceProperties targetDataSourceProperties = (WrapperTargetDataSourceProperties) hikariDataSource.getDataSourceProperties().get("targetDataSourceProperties");
        assertEquals(1, targetDataSourceProperties.size());
        assertEquals("plugin1", targetDataSourceProperties.getProperty("wrapperPlugins"));
    }

    @Test
    void createWithH2DoesNotConfigureAwsExceptionOverride() {
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setDriverClassName("org.h2.Driver");

        HikariDataSource hikariDataSource = HikariDataSourceFactory.create(
                dataSourceProperties, new WrapperTargetDataSourceProperties(), "sa", "jdbc:h2:mem:test");

        assertNull(hikariDataSource.getExceptionOverrideClassName());
    }
}
