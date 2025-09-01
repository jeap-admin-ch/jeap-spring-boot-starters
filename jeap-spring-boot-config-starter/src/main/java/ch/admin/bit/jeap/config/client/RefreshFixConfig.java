package ch.admin.bit.jeap.config.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.refresh.ConfigDataContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = RefreshAutoConfiguration.class)
@EnableConfigurationProperties(RefreshAutoConfiguration.RefreshProperties.class)
public class RefreshFixConfig {

    @Bean
    public ConfigDataContextRefresher configDataContextRefresher(ConfigurableApplicationContext context,
                                                                 RefreshScope scope, RefreshAutoConfiguration.RefreshProperties properties) {
        return new FixedConfigDataContextRefresher(context, scope, properties);
    }

    @Bean
    public static RefreshScope refreshScope() {
        return new RefreshScope();
    }

}
