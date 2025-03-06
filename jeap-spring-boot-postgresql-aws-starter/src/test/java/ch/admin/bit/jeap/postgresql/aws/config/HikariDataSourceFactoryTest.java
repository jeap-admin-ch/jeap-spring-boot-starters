package ch.admin.bit.jeap.postgresql.aws.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HikariDataSourceFactoryTest {

    @Test
    void createWithWrapper() {
        WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties = new WrapperTargetDataSourceProperties();
        wrapperTargetDataSourceProperties.put("wrapperPlugins", "plugin1");
        HikariDataSource hikariDataSource = HikariDataSourceFactory.create(new DataSourceProperties(), wrapperTargetDataSourceProperties,  "myUser", "jdbc:postgresql://localhost:5432/db");

        assertNotNull(hikariDataSource);
        assertEquals("jdbc:aws-wrapper:postgresql://localhost:5432/db", hikariDataSource.getDataSourceProperties().getProperty("jdbcUrl"));
        assertEquals("myUser", hikariDataSource.getUsername());
        WrapperTargetDataSourceProperties targetDataSourceProperties = (WrapperTargetDataSourceProperties) hikariDataSource.getDataSourceProperties().get("targetDataSourceProperties");
        assertEquals(1, targetDataSourceProperties.size());
        assertEquals("plugin1", targetDataSourceProperties.getProperty("wrapperPlugins"));
    }
}