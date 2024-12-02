package ch.admin.bit.jeap.postgresql.aws;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RDSDataSourceTest {


    @Test
    public void authorizationTokenIsGeneratedWhenNoPasswordIsSet() {
        AwsBasicCredentials basicCredentials = AwsBasicCredentials.create("myAccessKeyId", "mySecretAccessKey");
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(basicCredentials);

        RDSDataSource dataSource = new RDSDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://someproxyendpoint.rds.amazonaws.com:5432/app?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
        dataSource.setRegion(Region.EU_CENTRAL_2.id());
        dataSource.setUsername("user");
        dataSource.setCredentialsProvider(credentialsProvider);

        String authorizationToken = dataSource.getPassword();
        assertTrue(authorizationToken.contains("someproxyendpoint.rds.amazonaws.com:5432"));
        assertTrue(authorizationToken.contains("X-Amz-Credential=myAccessKeyId"));
    }

    @Test
    public void passwordIsReturnedWhenSet() {
        RDSDataSource dataSource = new RDSDataSource();
        dataSource.setPassword("myPass");

        assertEquals("myPass", dataSource.getPassword());
    }

    @Test
    void getHostnameReadsFromProperty() {
        RDSDataSource dataSource = new RDSDataSource();
        dataSource.setHostname("hostname");

        assertEquals("hostname", dataSource.getHostname());
    }

    @Test
    void getHostnameReadsFromJdbcUrlIfNotSet() {
        RDSDataSource dataSource = new RDSDataSource();

        dataSource.setJdbcUrl("jdbc:postgresql://someproxyendpoint.rds.amazonaws.com/app");
        assertEquals("someproxyendpoint.rds.amazonaws.com", dataSource.getHostname());

        dataSource.setJdbcUrl("jdbc:postgresql://someproxyendpoint.rds.amazonaws.com:5432/app");
        assertEquals("someproxyendpoint.rds.amazonaws.com", dataSource.getHostname());
    }

    @Test
    void getHostnameReturnsDefaultIfPropertyOrJdbcUrlNotSet() {
        RDSDataSource dataSource = new RDSDataSource();

        assertEquals(RDSDataSource.DEFAULT_HOSTNAME, dataSource.getHostname());
    }

    @Test
    void getPortReadsFromProperty() {
        RDSDataSource dataSource = new RDSDataSource();
        dataSource.setPort("12345");

        assertEquals(12345, dataSource.getPort());
    }

    @Test
    void getPortReadsFromJdbcUrlIfNotSet() {
        RDSDataSource dataSource = new RDSDataSource();

        dataSource.setJdbcUrl("jdbc:postgresql://someproxyendpoint.rds.amazonaws.com:42/app");
        assertEquals(42, dataSource.getPort());
    }

    @Test
    void getPortReturnsDefaultIfPropertyOrJdbcUrlNotSet() {
        RDSDataSource dataSource = new RDSDataSource();

        assertEquals(RDSDataSource.DEFAULT_PORT_NUMBER, dataSource.getPort());
    }

    @Test
    void getPortReturnsDefaultIfJdbcUrlDoesNotHaveIt() {
        RDSDataSource dataSource = new RDSDataSource();

        dataSource.setJdbcUrl("jdbc:postgresql://someproxyendpoint.rds.amazonaws.com/app");
        assertEquals(RDSDataSource.DEFAULT_PORT_NUMBER, dataSource.getPort());
    }

}