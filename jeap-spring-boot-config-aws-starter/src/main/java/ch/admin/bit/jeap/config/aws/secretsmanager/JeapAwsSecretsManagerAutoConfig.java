package ch.admin.bit.jeap.config.aws.secretsmanager;

import ch.admin.bit.jeap.config.aws.appconfig.JeapSpringApplicationProperties;
import ch.admin.bit.jeap.config.aws.secretsmanager.config.SpringCloudSecretsManagerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties({
        JeapAwsSecretsManagerProperties.class,
        SpringCloudSecretsManagerProperties.class,
        JeapSpringApplicationProperties.class})
@ConditionalOnProperty(name = {"jeap.config.aws.secretmanager.enabled", "spring.cloud.aws.secretmanager.enabled"},
        havingValue = "true", matchIfMissing = true)
public class JeapAwsSecretsManagerAutoConfig {

}
