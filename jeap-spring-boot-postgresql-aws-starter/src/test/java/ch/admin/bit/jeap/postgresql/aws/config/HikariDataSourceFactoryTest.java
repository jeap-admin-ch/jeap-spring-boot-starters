package ch.admin.bit.jeap.postgresql.aws.config;

import ch.admin.bit.jeap.postgresql.aws.RDSDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HikariDataSourceFactoryTest {

    @Test
    void createWithoutWrapper() {
        JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties = new JeapPostgreSQLAWSProperties();
        jeapPostgreSQLAWSProperties.setPort("555");
        WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties = new WrapperTargetDataSourceProperties();
        AwsCredentialsProvider awsCredentialsProvider = Mockito.mock(AwsCredentialsProvider.class);
        HikariDataSource hikariDataSource = HikariDataSourceFactory.create(new DataSourceProperties(), jeapPostgreSQLAWSProperties, wrapperTargetDataSourceProperties, awsCredentialsProvider, false, "myUser", "jdbc:postgresql://localhost:5432/db");

        assertNotNull(hikariDataSource);
        assertEquals("jdbc:postgresql://localhost:5432/db", hikariDataSource.getJdbcUrl());
        assertEquals("myUser", hikariDataSource.getUsername());
        RDSDataSource rdsDataSource = (RDSDataSource) hikariDataSource;
        assertEquals(awsCredentialsProvider, rdsDataSource.getCredentialsProvider());
        assertEquals(Region.EU_CENTRAL_2.id(), rdsDataSource.getRegion());
        assertEquals("localhost", rdsDataSource.getHostname());
        assertEquals(555, rdsDataSource.getPort());
    }

    @Test
    void createWithWrapper() {
        WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties = new WrapperTargetDataSourceProperties();
        wrapperTargetDataSourceProperties.put("wrapperPlugins", "plugin1");
        AwsCredentialsProvider awsCredentialsProvider = Mockito.mock(AwsCredentialsProvider.class);
        HikariDataSource hikariDataSource = HikariDataSourceFactory.create(new DataSourceProperties(), new JeapPostgreSQLAWSProperties(), wrapperTargetDataSourceProperties, awsCredentialsProvider, true, "myUser", "jdbc:postgresql://localhost:5432/db");

        assertNotNull(hikariDataSource);
        assertEquals("jdbc:aws-wrapper:postgresql://localhost:5432/db", hikariDataSource.getDataSourceProperties().getProperty("jdbcUrl"));
        assertEquals("myUser", hikariDataSource.getUsername());
        WrapperTargetDataSourceProperties targetDataSourceProperties = (WrapperTargetDataSourceProperties) hikariDataSource.getDataSourceProperties().get("targetDataSourceProperties");
        assertEquals(1, targetDataSourceProperties.size());
        assertEquals("plugin1", targetDataSourceProperties.getProperty("wrapperPlugins"));
    }
}