package ch.admin.bit.jeap.postgresql.aws.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import software.amazon.jdbc.ds.AwsWrapperDataSource;

@Slf4j
public class HikariDataSourceFactory {

    public static HikariDataSource create(DataSourceProperties properties, WrapperTargetDataSourceProperties wrapperTargetDataSourceProperties, String userName, String jdbcUrl) {
        if (!isTestDatabase(jdbcUrl, properties)) {
            log.info("Enabling AWS Advanced Jdbc Wrapper.");
            HikariDataSource ds = new HikariDataSource();
            ds.setUsername(userName);
            ds.setDataSourceClassName(AwsWrapperDataSource.class.getName());
            ds.addDataSourceProperty("jdbcUrl", jdbcUrl.replace("jdbc:postgresql", "jdbc:aws-wrapper:postgresql"));
            ds.addDataSourceProperty("targetDataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
            ds.addDataSourceProperty("targetDataSourceProperties", wrapperTargetDataSourceProperties);
            return ds;
        } else {
            // The AWS wrapper is not compatible with H2 databases, therefore we initialize a regular Hikari datasource
            // for H2 databases
            log.info("Detected test configuration database. Initializing a test H2 datasource.");
            properties.setUrl(jdbcUrl);
            properties.setUsername(userName);
            return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        }
    }

    private static boolean isTestDatabase(String jdbcUrl, DataSourceProperties properties) {
        return jdbcUrl.toLowerCase().contains("jdbc:h2:") || (properties.getDriverClassName() != null && properties.getDriverClassName().equals("org.h2.Driver"));
    }

}
