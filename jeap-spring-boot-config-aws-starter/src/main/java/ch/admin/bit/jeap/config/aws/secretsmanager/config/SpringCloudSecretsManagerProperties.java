package ch.admin.bit.jeap.config.aws.secretsmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * For backwards compatibility, respect the spring.cloud.aws.secretsmanager.enabled property if it is set
 */
@Data
@ConfigurationProperties(prefix = SpringCloudSecretsManagerProperties.CONFIG_PREFIX)
public class SpringCloudSecretsManagerProperties {

    public static final String CONFIG_PREFIX = "spring.cloud.aws.secretsmanager";

    private boolean enabled = true;
}
