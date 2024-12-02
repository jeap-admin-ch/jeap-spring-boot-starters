package ch.admin.bit.jeap.config.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.refresh.ConfigDataContextRefresher;
import org.springframework.cloud.context.refresh.LegacyContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.util.ConditionalOnBootstrapDisabled;
import org.springframework.cloud.util.ConditionalOnBootstrapEnabled;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = RefreshAutoConfiguration.class)
@EnableConfigurationProperties(RefreshAutoConfiguration.RefreshProperties.class)
public class RefreshFixConfig {

    /**
     * fixes only a refresh when using the bootstrap context, i.e. the LegacyContextRefresher
     */
    @ConditionalOnBootstrapEnabled
    @Bean
    public LegacyContextRefresher legacyContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
                                                         RefreshAutoConfiguration.RefreshProperties properties) {
        return new FixedLegacyContextRefresher(context, scope, properties);
    }

    /** fixes only a refresh without bootstrap context, i.e. the ConfigDataContextRefresher */
    @Bean
    @ConditionalOnBootstrapDisabled
    public ConfigDataContextRefresher configDataContextRefresher(ConfigurableApplicationContext context,
                                                                 RefreshScope scope, RefreshAutoConfiguration.RefreshProperties properties) {
        return new FixedConfigDataContextRefresher(context, scope, properties);
    }

    @Bean
    public static RefreshScope refreshScope() {
        return new RefreshScope();
    }

}
