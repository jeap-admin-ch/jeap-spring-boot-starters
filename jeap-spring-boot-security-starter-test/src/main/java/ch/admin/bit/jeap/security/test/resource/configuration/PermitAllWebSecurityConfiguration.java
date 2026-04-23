package ch.admin.bit.jeap.security.test.resource.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration
@SuppressWarnings("Convert2MethodRef")
public class PermitAllWebSecurityConfiguration {

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Configuration
    public static class ServletPermitAllWebSecurityConfiguration {
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 5)
        // Overrule other web security configurations, but still allow room for overriding.
        public SecurityFilterChain servletPermitAllSecurityFilterChain(HttpSecurity http) {
            http.
                    securityMatcher("/**").
                    csrf(csrf -> csrf.disable()).
                    authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.anyRequest().permitAll());
            return http.build();
        }
    }

}
