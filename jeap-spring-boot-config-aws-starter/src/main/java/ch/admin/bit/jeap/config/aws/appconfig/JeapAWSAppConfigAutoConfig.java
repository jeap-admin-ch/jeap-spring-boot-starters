package ch.admin.bit.jeap.config.aws.appconfig;

import ch.admin.bit.jeap.config.aws.appconfig.refresh.AppConfigContextRefresher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;


@AutoConfiguration
@EnableConfigurationProperties({JeapAWSAppConfigProperties.class, JeapSpringApplicationProperties.class})
public class JeapAWSAppConfigAutoConfig {

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AppConfigContextRefresher appConfigContextRefresher(ConfigurableEnvironment environment, ContextRefresher refresher) {
        return new AppConfigContextRefresher(environment, refresher);
    }
}
