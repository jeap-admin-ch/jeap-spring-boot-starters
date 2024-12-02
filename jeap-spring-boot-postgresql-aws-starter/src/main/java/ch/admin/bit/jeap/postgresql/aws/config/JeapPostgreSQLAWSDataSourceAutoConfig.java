package ch.admin.bit.jeap.postgresql.aws.config;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "jeap.postgresql.aws.enabled", havingValue = "true")
@PropertySource(value = {"classpath:jeap-postgresql-aws.properties"})
public class JeapPostgreSQLAWSDataSourceAutoConfig {

    @Bean
    @ConditionalOnMissingBean(AwsCredentialsProvider.class)
    DefaultCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    @Bean
    @ConfigurationProperties("jeap.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("jeap.datasource.aws")
    public JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties() {
        return new JeapPostgreSQLAWSProperties();
    }

    @Bean
    @ConfigurationProperties("jeap.datasource.aws.wrapper.target-data-source-properties")
    public WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties() {
        return new WrapperTargetDataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("jeap.datasource.hikari")
    public HikariDataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties properties,
                                       JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties,
                                       @Qualifier("wrapperTargetDataSourceProperties") WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties,
                                       AwsCredentialsProvider awsCredentialsProvider,
                                       @Value("${spring.application.name:}") String applicationName,
                                       @Value("${jeap.datasource.aws.enable-advanced-jdbc-wrapper:true}") boolean enableAdvancedJdbcWrapper) {
        String inferredUsername = inferUsername(properties, applicationName);
        log.info("Inferred datasource username: {}", inferredUsername);

        String inferredJdbcUrl = inferJdbcUrl(properties, jeapPostgreSQLAWSProperties, applicationName);
        log.info("Inferred Jdbc Url: {}", inferredJdbcUrl);

        return HikariDataSourceFactory.create(properties, jeapPostgreSQLAWSProperties, wrapperTargetDataSourceProperties, awsCredentialsProvider, enableAdvancedJdbcWrapper, inferredUsername, inferredJdbcUrl);
    }

    @Bean
    @Primary
    public ReadReplicaAwareTransactionRoutingDataSource transactionRoutingDataSource(@Qualifier("dataSource") DataSource dataSource, @Autowired(required = false) @Qualifier("replicaDataSource") DataSource replicaDataSource) {
        ReadReplicaAwareTransactionRoutingDataSource routingDataSource = new ReadReplicaAwareTransactionRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(ReadReplicaAwareTransactionRoutingDataSource.WRITER_KEY, dataSource);

        if (replicaDataSource != null) {
            dataSourceMap.put(ReadReplicaAwareTransactionRoutingDataSource.READER_KEY, replicaDataSource);
        }

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(dataSource);
        return routingDataSource;
    }

    static String inferUsername(DataSourceProperties properties, String applicationName) {
        return hasText(properties.getUsername()) ? properties.getUsername() : normalizeName(applicationName) + "_db_rwa";
    }

    static String inferJdbcUrl(DataSourceProperties properties, JeapPostgreSQLAWSProperties jeapPostgreSQLAWSProperties, String applicationName) {
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
            } else {
                jdbcUrlBuilder.append(normalizeName(applicationName)).append("_db");
            }
            jdbcUrlBuilder.append("?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
            return jdbcUrlBuilder.toString();
        }
        throw new IllegalStateException("Expected a Jdbc Url via the property 'jeap.datasource.url' or a hostname via 'jeap.datasource.aws.hostname'");
    }

    private static String normalizeName(String applicationName) {
        return applicationName.toLowerCase().replace("-", "_");
    }
}
