package ch.admin.bit.jeap.security.resource.token;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class TokenConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthoritiesResolver authoritiesResolver() {
        return new DefaultAuthoritiesResolver();
    }
}
