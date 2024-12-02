package ch.admin.bit.jeap.postgresql.aws.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class JeapPostgreSQLAWSDataSourceAutoConfigTest {

    @Test
    void dataSource_whenHostnameIsSet_thenHasMostPrecedence() {
        DataSourceProperties dataSourceProperties = Mockito.mock(DataSourceProperties.class);
        JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties = Mockito.mock(JeapPostgreSQLAWSProperties.class);

        when(jeapPostgreSQLAWSProperties.getHostname()).thenReturn("myHostname");
        when(jeapPostgreSQLAWSProperties.getPort()).thenReturn("5432");

        String jdbcUrl = JeapPostgreSQLAWSDataSourceAutoConfig.inferJdbcUrl(dataSourceProperties, jeapPostgreSQLAWSProperties, "my-app");
        assertEquals("jdbc:postgresql://myHostname:5432/my_app_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory", jdbcUrl);
    }

    @Test
    void dataSource_whenNoHostnameIsSet_thenJdbcUrlIsUsed() {
        DataSourceProperties dataSourceProperties = Mockito.mock(DataSourceProperties.class);
        JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties = Mockito.mock(JeapPostgreSQLAWSProperties.class);

        when(dataSourceProperties.getUrl()).thenReturn("jdbc:postgresql://dummy");
        when(jeapPostgreSQLAWSProperties.getHostname()).thenReturn(null);

        String jdbcUrl = JeapPostgreSQLAWSDataSourceAutoConfig.inferJdbcUrl(dataSourceProperties, jeapPostgreSQLAWSProperties, "my-app");
        assertEquals("jdbc:postgresql://dummy", jdbcUrl);
    }

    @Test
    void dataSource_whenUsernameIsSet_thenHasMostPrecedence() {
        DataSourceProperties dataSourceProperties = Mockito.mock(DataSourceProperties.class);

        when(dataSourceProperties.getUsername()).thenReturn("myUser");

        String username = JeapPostgreSQLAWSDataSourceAutoConfig.inferUsername(dataSourceProperties, "my-app");
        assertEquals("myUser", username);
    }

    @Test
    void dataSource_whenNoUsernameIsSet_thenDefaultsAreApplied() {
        DataSourceProperties dataSourceProperties = Mockito.mock(DataSourceProperties.class);

        when(dataSourceProperties.getUsername()).thenReturn(null);

        String username = JeapPostgreSQLAWSDataSourceAutoConfig.inferUsername(dataSourceProperties, "my-app");
        assertEquals("my_app_db_rwa", username);
    }

    @Test
    void dataSource_whenDatabaseNameIsSet_thenIsInFinalJdbcUrl() {
        DataSourceProperties dataSourceProperties = Mockito.mock(DataSourceProperties.class);
        JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties = Mockito.mock(JeapPostgreSQLAWSProperties.class);

        when(dataSourceProperties.getUrl()).thenReturn(null);
        when(jeapPostgreSQLAWSProperties.getHostname()).thenReturn("hostname");
        when(jeapPostgreSQLAWSProperties.getPort()).thenReturn("5432");
        when(jeapPostgreSQLAWSProperties.getDatabaseName()).thenReturn("chocolate_factory_db");

        String jdbcUrl = JeapPostgreSQLAWSDataSourceAutoConfig.inferJdbcUrl(dataSourceProperties, jeapPostgreSQLAWSProperties, "my-app");
        assertEquals("jdbc:postgresql://hostname:5432/chocolate_factory_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory", jdbcUrl);
    }

}
