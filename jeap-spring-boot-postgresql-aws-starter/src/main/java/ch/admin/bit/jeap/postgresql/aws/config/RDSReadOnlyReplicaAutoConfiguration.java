package ch.admin.bit.jeap.postgresql.aws.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import static org.springframework.util.StringUtils.hasText;

/**
 * If environment configuration matches, this configuration class will initialize a second data source meant for
 * read-only transactions only. By default, when using a single read/write endpoint in RDS, RDS Proxy will route
 * ALL requests to the writer instance. In order to offload the writer instance, an additional readonly endpoint can
 * be provisioned which will route requests to readonly replicas.
 * <p>
 * More info: <a href="https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/rds-proxy-endpoints.html#rds-proxy-endpoints-reader">AWS RDS</a>
 */
@Slf4j
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnProperty(name = "jeap.datasource.replica.enabled", havingValue = "true")
public class RDSReadOnlyReplicaAutoConfiguration {

    @Bean
    @ConfigurationProperties("jeap.datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("jeap.datasource.replica.aws")
    public JeapPostgreSQLAWSProperties replicaJeapPostgreSQLAWSProperties() {
        return new JeapPostgreSQLAWSProperties();
    }

    @Bean
    @ConfigurationProperties("jeap.datasource.replica.hikari")
    public HikariDataSource replicaDataSource(@Qualifier("replicaDataSourceProperties") DataSourceProperties properties,
                                           @Qualifier("replicaJeapPostgreSQLAWSProperties") JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties,
                                           @Qualifier("wrapperTargetDataSourceProperties") WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties,
                                           AwsCredentialsProvider awsCredentialsProvider,
                                           @Value("${spring.application.name:}") String applicationName,
                                           @Value("${jeap.datasource.aws.database-name:}") String databaseName,
                                           @Value("${jeap.datasource.aws.enable-advanced-jdbc-wrapper:true}") boolean enableAdvancedJdbcWrapper) {
        String inferredUsername = inferUsername(properties, applicationName);
        log.info("Inferred replica datasource username: {}", inferredUsername);

        String inferredJdbcUrl = inferJdbcUrl(properties, jeapPostgreSQLAWSProperties, applicationName, databaseName);
        log.info("Inferred replica Jdbc Url: {}", inferredJdbcUrl);

        return HikariDataSourceFactory.create(properties, jeapPostgreSQLAWSProperties, wrapperTargetDataSourceProperties, awsCredentialsProvider, enableAdvancedJdbcWrapper, inferredUsername, inferredJdbcUrl);
    }

    static String inferJdbcUrl(DataSourceProperties properties, JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties, String applicationName, String databaseName) {
        if (hasText(properties.getUrl())) {
            return properties.getUrl();
        } else if (hasText(jeapPostgreSQLAWSProperties.getHostname())) {
            StringBuilder jdbcUrlBuilder = new StringBuilder();
            jdbcUrlBuilder.append("jdbc:postgresql://");
            jdbcUrlBuilder.append(jeapPostgreSQLAWSProperties.getHostname());
            jdbcUrlBuilder.append(":");
            jdbcUrlBuilder.append(jeapPostgreSQLAWSProperties.getPort());
            jdbcUrlBuilder.append("/");
            if (hasText(jeapPostgreSQLAWSProperties.getDatabaseName())) {
                jdbcUrlBuilder.append(jeapPostgreSQLAWSProperties.getDatabaseName());
            } else if (hasText(databaseName)) {
                jdbcUrlBuilder.append(databaseName);
            } else {
                jdbcUrlBuilder.append(normalizeName(applicationName)).append("_db");
            }
            jdbcUrlBuilder.append("?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
            return jdbcUrlBuilder.toString();
        }
        throw new IllegalStateException("Expected a Jdbc Url via the property 'jeap.datasource.replica.url' or a hostname via 'jeap.datasource.replica.aws.hostname'");
    }

    static String inferUsername(DataSourceProperties properties, String applicationName) {
        return hasText(properties.getUsername()) ? properties.getUsername() : normalizeName(applicationName) + "_db_ro";
    }

    private static String normalizeName(String applicationName) {
        return applicationName.toLowerCase().replace("-", "_");
    }

}
