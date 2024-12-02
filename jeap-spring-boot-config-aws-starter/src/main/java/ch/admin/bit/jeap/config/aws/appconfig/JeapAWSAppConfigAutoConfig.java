package ch.admin.bit.jeap.config.aws.appconfig;

import ch.admin.bit.jeap.config.aws.appconfig.refresh.AppConfigContextRefresher;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;


@AutoConfiguration(before = CredentialsProviderAutoConfiguration.class)
@EnableConfigurationProperties({JeapAWSAppConfigProperties.class, JeapSpringApplicationProperties.class})
public class JeapAWSAppConfigAutoConfig {

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AppConfigContextRefresher appConfigContextRefresher(ConfigurableEnvironment environment, ContextRefresher refresher) {
        return new AppConfigContextRefresher(environment, refresher);
    }

    /**
     * {@link CredentialsProviderAutoConfiguration} is @ConditionalOnMissingBean(AwsCredentialsProvider.class). We do
     * not want the auto configuration to be applied as it applies too much magic. I.e. it will require an AWS region
     * to be set when AWS Security Token Service (STS) dependencies are on the classpath (which is the case i.e. for
     * AWS Glue Schema Registry access with jeap-messaging).
     */
    @Bean
    @ConditionalOnMissingBean(AwsCredentialsProvider.class)
    DefaultCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.create();
    }
}
