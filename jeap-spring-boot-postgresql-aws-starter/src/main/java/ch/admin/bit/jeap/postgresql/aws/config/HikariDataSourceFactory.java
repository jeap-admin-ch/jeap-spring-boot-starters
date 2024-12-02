package ch.admin.bit.jeap.postgresql.aws.config;

import ch.admin.bit.jeap.postgresql.aws.RDSDataSource;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.jdbc.ds.AwsWrapperDataSource;

@Slf4j
public class HikariDataSourceFactory {

    public static HikariDataSource create(DataSourceProperties properties, JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties, WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties, AwsCredentialsProvider awsCredentialsProvider, boolean enableAdvancedJdbcWrapper, String userName, String jdbcUrl) {
        if (enableAdvancedJdbcWrapper) {
            log.info("Enabling AWS Advanced Jdbc Wrapper.");
            HikariDataSource ds = new HikariDataSource();
            ds.setUsername(userName);
            ds.setDataSourceClassName(AwsWrapperDataSource.class.getName());
            ds.addDataSourceProperty("jdbcUrl", jdbcUrl.replace("jdbc:postgresql", "jdbc:aws-wrapper:postgresql"));
            ds.addDataSourceProperty("targetDataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
            ds.addDataSourceProperty("targetDataSourceProperties", wrapperTargetDataSourceProperties);
            return ds;
        } else {
            log.info("AWS Advanced Jdbc Wrapper is not enabled.");
            properties.setUrl(jdbcUrl);
            properties.setUsername(userName);
            RDSDataSource rdsDataSource = properties.initializeDataSourceBuilder().type(RDSDataSource.class).build();

            rdsDataSource.setCredentialsProvider(awsCredentialsProvider);
            rdsDataSource.setRegion(jeapPostgreSQLAWSProperties.getRegion());
            rdsDataSource.setHostname(jeapPostgreSQLAWSProperties.getHostname());
            rdsDataSource.setPort(jeapPostgreSQLAWSProperties.getPort());
            return rdsDataSource;
        }
    }

}
