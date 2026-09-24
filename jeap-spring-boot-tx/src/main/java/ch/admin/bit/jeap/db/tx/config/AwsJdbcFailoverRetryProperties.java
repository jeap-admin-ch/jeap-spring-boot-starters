package ch.admin.bit.jeap.db.tx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("jeap.datasource.aws.failover-retry")
public record AwsJdbcFailoverRetryProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("2") int maxAttempts,
        @DefaultValue("100") long backoffMillis) {
}
